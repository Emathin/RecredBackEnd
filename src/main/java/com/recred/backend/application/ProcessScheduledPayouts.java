package com.recred.backend.application;

import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.KiosqueroPayoutConfig;
import com.recred.backend.domain.models.PaymentDetails;
import com.recred.backend.domain.models.PaymentStatus;
import com.recred.backend.domain.models.PayoutScheduleStatus;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.repositories.KiosqueroPayoutConfigRepository;
import com.recred.backend.domain.repositories.KiosqueroRepository;
import com.recred.backend.domain.repositories.PurchaseRepository;
import com.recred.backend.domain.services.PayoutPaymentService;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessScheduledPayouts {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessScheduledPayouts.class);
    private static final int DEFAULT_LOOKBACK_DAYS = 30;

    private final KiosqueroPayoutConfigRepository repository;
    private final PayoutPaymentService paymentService;
    private final KiosqueroRepository kiosqueroRepository;
    private final PurchaseRepository purchaseRepository;
    private final Clock clock;

    public ProcessScheduledPayouts(
            KiosqueroPayoutConfigRepository repository,
            PayoutPaymentService paymentService,
            KiosqueroRepository kiosqueroRepository,
            PurchaseRepository purchaseRepository) {
        this(repository, paymentService, kiosqueroRepository, purchaseRepository,
                Clock.systemDefaultZone());
    }

    public ProcessScheduledPayouts(
            KiosqueroPayoutConfigRepository repository,
            PayoutPaymentService paymentService,
            KiosqueroRepository kiosqueroRepository,
            PurchaseRepository purchaseRepository,
            Clock clock) {
        this.repository = repository;
        this.paymentService = paymentService;
        this.kiosqueroRepository = kiosqueroRepository;
        this.purchaseRepository = purchaseRepository;
        this.clock = clock;
    }

    public void execute() {
        LocalDate today = LocalDate.now(clock);
        List<KiosqueroPayoutConfig> dueConfigs = repository.findActiveConfigsDueOnOrBefore(today);
        dueConfigs.forEach(config -> processConfig(config, today));
    }

    private void processConfig(KiosqueroPayoutConfig config, LocalDate today) {
        UUID buffetId = kiosqueroRepository.findBuffetById(config.getKiosqueroId());
        if (buffetId == null) {
            return;
        }

        BigDecimal totalSales = totalDeliveredSales(config, buffetId, today);
        if (hasNoSales(totalSales)) {
            markExecuted(config, today);
            repository.save(config);
            return;
        }

        transferSales(config, totalSales, today);
        repository.save(config);
    }

    private BigDecimal totalDeliveredSales(
            KiosqueroPayoutConfig config,
            UUID buffetId,
            LocalDate today) {
        return purchaseRepository.findByBuffetAndDateRange(
                        buffetId,
                        startDate(config),
                        today.atTime(LocalTime.MAX)
                )
                .stream()
                .filter(purchase -> purchase.getStatus() == EstadoCompra.ENTREGADO)
                .map(Purchase::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private LocalDateTime startDate(KiosqueroPayoutConfig config) {
        if (config.getUltimaEjecucion() != null) {
            return config.getUltimaEjecucion().atStartOfDay();
        }
        if (config.getCreadoEn() != null) {
            return config.getCreadoEn();
        }
        return LocalDateTime.now(clock).minusDays(DEFAULT_LOOKBACK_DAYS);
    }

    private boolean hasNoSales(BigDecimal totalSales) {
        return totalSales.compareTo(BigDecimal.ZERO) <= 0;
    }

    private void transferSales(
            KiosqueroPayoutConfig config,
            BigDecimal totalSales,
            LocalDate today) {
        try {
            PaymentDetails details = paymentService.transferToThirdParty(
                    config.getDestinationCvu(),
                    config.getDestinationCuit(),
                    config.getAccountHolderName(),
                    totalSales,
                    idempotencyKey(config)
            );
            updateConfigAfterTransfer(config, details, today);
        } catch (RuntimeException exception) {
            LOGGER.warn("scheduled_payout_transfer_failed kiosqueroId={}",
                    config.getKiosqueroId(), exception);
        }
    }

    private String idempotencyKey(KiosqueroPayoutConfig config) {
        String combinedKey = config.getKiosqueroId()
                + "_" + config.getProximaEjecucion();
        return UUID.nameUUIDFromBytes(combinedKey.getBytes(StandardCharsets.UTF_8)).toString();
    }

    private void updateConfigAfterTransfer(
            KiosqueroPayoutConfig config,
            PaymentDetails details,
            LocalDate today) {
        if (details.status() == PaymentStatus.APPROVED) {
            markExecuted(config, today);
            return;
        }
        config.setEstado(PayoutScheduleStatus.PAUSED);
    }

    private void markExecuted(KiosqueroPayoutConfig config, LocalDate today) {
        config.setUltimaEjecucion(today);
        config.calculateNextPayout(today);
    }
}
