package com.recred.backend.unit.application;

import com.recred.backend.application.ProcessScheduledPayouts;
import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.IntervalUnit;
import com.recred.backend.domain.models.KiosqueroPayoutConfig;
import com.recred.backend.domain.models.PaymentDetails;
import com.recred.backend.domain.models.PaymentStatus;
import com.recred.backend.domain.models.PayoutScheduleStatus;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.repositories.KiosqueroPayoutConfigRepository;
import com.recred.backend.domain.repositories.KiosqueroRepository;
import com.recred.backend.domain.repositories.PurchaseRepository;
import com.recred.backend.domain.services.PayoutPaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessScheduledPayoutsTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 7, 2);
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-07-02T10:00:00Z"),
            ZoneOffset.UTC
    );

    @Mock
    private KiosqueroPayoutConfigRepository repository;
    @Mock
    private PayoutPaymentService paymentService;
    @Mock
    private KiosqueroRepository kiosqueroRepository;
    @Mock
    private PurchaseRepository purchaseRepository;

    private ProcessScheduledPayouts useCase;

    @BeforeEach
    void setUp() {
        useCase = new ProcessScheduledPayouts(
                repository,
                paymentService,
                kiosqueroRepository,
                purchaseRepository,
                CLOCK
        );
    }

    @Test
    void executeWhenKiosqueroHasNoBuffetShouldSkipConfig() {
        UUID kiosqueroId = UUID.randomUUID();
        KiosqueroPayoutConfig config = activeConfig(kiosqueroId);
        when(repository.findActiveConfigsDueOnOrBefore(TODAY)).thenReturn(List.of(config));
        when(kiosqueroRepository.findBuffetById(kiosqueroId)).thenReturn(null);

        useCase.execute();

        verify(repository).findActiveConfigsDueOnOrBefore(TODAY);
        verify(kiosqueroRepository).findBuffetById(kiosqueroId);
        verify(repository, never()).save(config);
        verifyNoInteractions(purchaseRepository, paymentService);
    }

    @Test
    void executeWhenConfigHasNoDeliveredSalesShouldMarkExecuted() {
        UUID kiosqueroId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        KiosqueroPayoutConfig config = activeConfig(kiosqueroId);
        config.setUltimaEjecucion(TODAY.minusDays(5));
        when(repository.findActiveConfigsDueOnOrBefore(TODAY)).thenReturn(List.of(config));
        when(kiosqueroRepository.findBuffetById(kiosqueroId)).thenReturn(buffetId);
        when(purchaseRepository.findByBuffetAndDateRange(
                buffetId,
                TODAY.minusDays(5).atStartOfDay(),
                TODAY.atTime(LocalTime.MAX)
        )).thenReturn(List.of(purchase(EstadoCompra.PENDIENTE, "120.00")));

        useCase.execute();

        assertEquals(TODAY, config.getUltimaEjecucion());
        assertEquals(TODAY.plusDays(1), config.getProximaEjecucion());
        verify(repository).save(config);
        verifyNoInteractions(paymentService);
    }

    @Test
    void executeWhenTransferIsApprovedShouldMarkExecutedAndSave() {
        UUID kiosqueroId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        LocalDate nextExecution = TODAY;
        LocalDateTime createdAt = TODAY.minusDays(2).atTime(8, 30);
        KiosqueroPayoutConfig config = activeConfig(kiosqueroId);
        config.setCreadoEn(createdAt);
        when(repository.findActiveConfigsDueOnOrBefore(TODAY)).thenReturn(List.of(config));
        when(kiosqueroRepository.findBuffetById(kiosqueroId)).thenReturn(buffetId);
        when(purchaseRepository.findByBuffetAndDateRange(
                buffetId,
                createdAt,
                TODAY.atTime(LocalTime.MAX)
        )).thenReturn(List.of(
                purchase(EstadoCompra.ENTREGADO, "100.00"),
                purchase(EstadoCompra.PENDIENTE, "30.00"),
                purchase(EstadoCompra.ENTREGADO, "50.50")
        ));
        when(paymentService.transferToThirdParty(
                eq("0000003100010000000001"),
                eq("20123456789"),
                eq("Cuenta Test"),
                eq(new BigDecimal("150.50")),
                eq(idempotencyKey(kiosqueroId, nextExecution))
        )).thenReturn(new PaymentDetails(
                "payment-id",
                null,
                new BigDecimal("150.50"),
                PaymentStatus.APPROVED
        ));

        useCase.execute();

        assertEquals(TODAY, config.getUltimaEjecucion());
        assertEquals(TODAY.plusDays(1), config.getProximaEjecucion());
        verify(repository).save(config);
    }

    @Test
    void executeWhenTransferIsRejectedShouldPauseConfig() {
        UUID kiosqueroId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        KiosqueroPayoutConfig config = activeConfig(kiosqueroId);
        when(repository.findActiveConfigsDueOnOrBefore(TODAY)).thenReturn(List.of(config));
        when(kiosqueroRepository.findBuffetById(kiosqueroId)).thenReturn(buffetId);
        when(purchaseRepository.findByBuffetAndDateRange(
                buffetId,
                CLOCK.instant().atZone(CLOCK.getZone()).toLocalDateTime().minusDays(30),
                TODAY.atTime(LocalTime.MAX)
        )).thenReturn(List.of(purchase(EstadoCompra.ENTREGADO, "15.00")));
        when(paymentService.transferToThirdParty(
                eq("0000003100010000000001"),
                eq("20123456789"),
                eq("Cuenta Test"),
                eq(new BigDecimal("15.00")),
                eq(idempotencyKey(kiosqueroId, TODAY))
        )).thenReturn(new PaymentDetails(
                "payment-id",
                null,
                new BigDecimal("15.00"),
                PaymentStatus.REJECTED
        ));

        useCase.execute();

        assertEquals(PayoutScheduleStatus.PAUSED, config.getEstado());
        assertNull(config.getUltimaEjecucion());
        assertEquals(TODAY, config.getProximaEjecucion());
        verify(repository).save(config);
    }

    @Test
    void executeWhenTransferFailsShouldKeepConfigActiveAndSave() {
        UUID kiosqueroId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        KiosqueroPayoutConfig config = activeConfig(kiosqueroId);
        config.setUltimaEjecucion(TODAY.minusDays(1));
        when(repository.findActiveConfigsDueOnOrBefore(TODAY)).thenReturn(List.of(config));
        when(kiosqueroRepository.findBuffetById(kiosqueroId)).thenReturn(buffetId);
        when(purchaseRepository.findByBuffetAndDateRange(
                buffetId,
                TODAY.minusDays(1).atStartOfDay(),
                TODAY.atTime(LocalTime.MAX)
        )).thenReturn(List.of(purchase(EstadoCompra.ENTREGADO, "40.00")));
        when(paymentService.transferToThirdParty(
                eq("0000003100010000000001"),
                eq("20123456789"),
                eq("Cuenta Test"),
                eq(new BigDecimal("40.00")),
                eq(idempotencyKey(kiosqueroId, TODAY))
        )).thenThrow(new RuntimeException("provider unavailable"));

        useCase.execute();

        assertEquals(PayoutScheduleStatus.ACTIVE, config.getEstado());
        assertEquals(TODAY.minusDays(1), config.getUltimaEjecucion());
        assertEquals(TODAY, config.getProximaEjecucion());
        verify(repository).save(config);
    }

    private KiosqueroPayoutConfig activeConfig(UUID kiosqueroId) {
        KiosqueroPayoutConfig config = new KiosqueroPayoutConfig();
        config.setKiosqueroId(kiosqueroId);
        config.setDestinationCvu("0000003100010000000001");
        config.setDestinationCuit("20123456789");
        config.setAccountHolderName("Cuenta Test");
        config.setCantidadIntervalo(1);
        config.setUnidadIntervalo(IntervalUnit.DAYS);
        config.setEstado(PayoutScheduleStatus.ACTIVE);
        config.setProximaEjecucion(TODAY);
        return config;
    }

    private Purchase purchase(EstadoCompra status, String amount) {
        Purchase purchase = new Purchase();
        purchase.setStatus(status);
        purchase.setTotalAmount(new BigDecimal(amount));
        return purchase;
    }

    private String idempotencyKey(UUID kiosqueroId, LocalDate proximaEjecucion) {
        String combinedKey = kiosqueroId + "_" + proximaEjecucion;
        return UUID.nameUUIDFromBytes(combinedKey.getBytes(StandardCharsets.UTF_8)).toString();
    }
}
