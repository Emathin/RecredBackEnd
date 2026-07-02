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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public class ProcessScheduledPayouts {
    private final KiosqueroPayoutConfigRepository repository;
    private final PayoutPaymentService paymentService;
    private final KiosqueroRepository kiosqueroRepository;
    private final PurchaseRepository purchaseRepository;

    public ProcessScheduledPayouts(
            KiosqueroPayoutConfigRepository repository, 
            PayoutPaymentService paymentService,
            KiosqueroRepository kiosqueroRepository,
            PurchaseRepository purchaseRepository) {
        this.repository = repository;
        this.paymentService = paymentService;
        this.kiosqueroRepository = kiosqueroRepository;
        this.purchaseRepository = purchaseRepository;
    }

    public void execute() {
        LocalDate today = LocalDate.now();
        List<KiosqueroPayoutConfig> dueConfigs = repository.findActiveConfigsDueOnOrBefore(today);

        for (KiosqueroPayoutConfig config : dueConfigs) {
            UUID buffetId = kiosqueroRepository.findBuffetById(config.getKiosqueroId());
            if (buffetId == null) {
                continue;
            }

            LocalDateTime start = config.getUltimaEjecucion() != null 
                    ? config.getUltimaEjecucion().atStartOfDay() 
                    : (config.getCreadoEn() != null 
                            ? config.getCreadoEn() 
                            : LocalDateTime.now().minusDays(30));
            LocalDateTime end = today.atTime(LocalTime.MAX);

            List<Purchase> purchases = purchaseRepository.findByBuffetAndDateRange(
                    buffetId, 
                    start, 
                    end
            );

            BigDecimal totalVentas = purchases.stream()
                    .filter(p -> p.getStatus() == EstadoCompra.ENTREGADO)
                    .map(Purchase::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (totalVentas.compareTo(BigDecimal.ZERO) <= 0) {
                config.setUltimaEjecucion(today);
                config.calculateNextPayout(today);
                repository.save(config);
                continue;
            }

            String combinedKey = config.getKiosqueroId().toString() 
                    + "_" + config.getProximaEjecucion().toString();
            String idempotencyKey = UUID.nameUUIDFromBytes(combinedKey.getBytes()).toString();

            try {
                PaymentDetails details = paymentService.transferToThirdParty(
                    config.getDestinationCvu(),
                    config.getDestinationCuit(),
                    config.getAccountHolderName(),
                    totalVentas,
                    idempotencyKey
                );

                if (details.status() == PaymentStatus.APPROVED) {
                    config.setUltimaEjecucion(today);
                    config.calculateNextPayout(today);
                } else {
                    config.setEstado(PayoutScheduleStatus.PAUSED);
                }
            } catch (Exception e) {
                // Errores de infraestructura: se mantiene activa la programación para que reintente
            }
            repository.save(config);
        }
    }
}
