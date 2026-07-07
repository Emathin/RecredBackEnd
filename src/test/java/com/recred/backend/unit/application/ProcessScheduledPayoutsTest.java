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

    private static final UUID KIOSQUERO_ID = UUID.randomUUID();
    private static final UUID BUFFET_ID = UUID.randomUUID();
    private static final String CVU = "0000003100010000000001";
    private static final String CUIT = "20123456789";
    private static final String HOLDER = "Cuenta Test";
    private static final String PAYMENT_ID = "payment-id";

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
    void execute_WhenKiosqueroHasNoBuffet_ShouldSkipConfig() {
        KiosqueroPayoutConfig config = givenActiveConfig();
        givenDueConfigsFound(config);
        givenKiosqueroHasNoBuffet();

        whenExecute();

        thenConfigIsSkippedAndNotSaved(config);
        thenNoInteractionsWithPurchaseAndPayment();
    }

    @Test
    void execute_WhenConfigHasNoDeliveredSales_ShouldMarkExecuted() {
        KiosqueroPayoutConfig config = givenActiveConfigWithLastExecution(TODAY.minusDays(5));
        givenDueConfigsFound(config);
        givenKiosqueroHasBuffet();
        givenNoDeliveredSalesFound(TODAY.minusDays(5).atStartOfDay());

        whenExecute();

        thenConfigIsMarkedAsExecutedAndSaved(config);
        thenPaymentServiceIsNotCalled();
    }

    @Test
    void execute_WhenTransferIsApproved_ShouldMarkExecutedAndSave() {
        LocalDateTime createdAt = TODAY.minusDays(2).atTime(8, 30);
        KiosqueroPayoutConfig config = givenActiveConfigWithCreatedAt(createdAt);
        givenDueConfigsFound(config);
        givenKiosqueroHasBuffet();
        givenDeliveredSalesFound(createdAt, "150.50");
        givenPaymentTransferIsApproved(config, "150.50");

        whenExecute();

        thenConfigIsMarkedAsExecutedAndSaved(config);
    }

    @Test
    void execute_WhenTransferIsRejected_ShouldPauseConfig() {
        KiosqueroPayoutConfig config = givenActiveConfig();
        givenDueConfigsFound(config);
        givenKiosqueroHasBuffet();
        LocalDateTime lookbackStart = CLOCK.instant().atZone(CLOCK.getZone())
                .toLocalDateTime().minusDays(30);
        givenDeliveredSalesFound(lookbackStart, "15.00");
        givenPaymentTransferIsRejected(config, "15.00");

        whenExecute();

        thenConfigIsPausedAndSaved(config);
    }

    @Test
    void execute_WhenTransferFails_ShouldKeepConfigActiveAndSave() {
        KiosqueroPayoutConfig config = givenActiveConfigWithLastExecution(TODAY.minusDays(1));
        givenDueConfigsFound(config);
        givenKiosqueroHasBuffet();
        givenDeliveredSalesFound(TODAY.minusDays(1).atStartOfDay(), "40.00");
        givenPaymentTransferFails(config, "40.00");

        whenExecute();

        thenConfigIsKeptActiveAndSaved(config, TODAY.minusDays(1));
    }

    private KiosqueroPayoutConfig givenActiveConfig() {
        KiosqueroPayoutConfig config = new KiosqueroPayoutConfig();
        config.setKiosqueroId(KIOSQUERO_ID);
        config.setDestinationCvu(CVU);
        config.setDestinationCuit(CUIT);
        config.setAccountHolderName(HOLDER);
        config.setCantidadIntervalo(1);
        config.setUnidadIntervalo(IntervalUnit.DAYS);
        config.setEstado(PayoutScheduleStatus.ACTIVE);
        config.setProximaEjecucion(TODAY);
        return config;
    }

    private KiosqueroPayoutConfig givenActiveConfigWithLastExecution(LocalDate lastExecution) {
        KiosqueroPayoutConfig config = givenActiveConfig();
        config.setUltimaEjecucion(lastExecution);
        return config;
    }

    private KiosqueroPayoutConfig givenActiveConfigWithCreatedAt(LocalDateTime createdAt) {
        KiosqueroPayoutConfig config = givenActiveConfig();
        config.setCreadoEn(createdAt);
        return config;
    }

    private void givenDueConfigsFound(KiosqueroPayoutConfig config) {
        when(repository.findActiveConfigsDueOnOrBefore(TODAY)).thenReturn(List.of(config));
    }

    private void givenKiosqueroHasNoBuffet() {
        when(kiosqueroRepository.findBuffetById(KIOSQUERO_ID)).thenReturn(null);
    }

    private void givenKiosqueroHasBuffet() {
        when(kiosqueroRepository.findBuffetById(KIOSQUERO_ID)).thenReturn(BUFFET_ID);
    }

    private void givenNoDeliveredSalesFound(LocalDateTime startDate) {
        when(purchaseRepository.findByBuffetAndDateRange(
                BUFFET_ID, startDate, TODAY.atTime(LocalTime.MAX)
        )).thenReturn(List.of(purchase(EstadoCompra.PENDIENTE, "120.00")));
    }

    private void givenDeliveredSalesFound(LocalDateTime startDate, String deliveredAmount) {
        when(purchaseRepository.findByBuffetAndDateRange(
                BUFFET_ID, startDate, TODAY.atTime(LocalTime.MAX)
        )).thenReturn(List.of(
                purchase(EstadoCompra.ENTREGADO, deliveredAmount),
                purchase(EstadoCompra.PENDIENTE, "30.00")
        ));
    }

    private void givenPaymentTransferIsApproved(KiosqueroPayoutConfig config, String amount) {
        when(paymentService.transferToThirdParty(
                eq(CVU), eq(CUIT), eq(HOLDER), eq(new BigDecimal(amount)),
                eq(idempotencyKey(config.getKiosqueroId(), config.getProximaEjecucion()))
        )).thenReturn(new PaymentDetails(
                PAYMENT_ID, null, new BigDecimal(amount), PaymentStatus.APPROVED
        ));
    }

    private void givenPaymentTransferIsRejected(KiosqueroPayoutConfig config, String amount) {
        when(paymentService.transferToThirdParty(
                eq(CVU), eq(CUIT), eq(HOLDER), eq(new BigDecimal(amount)),
                eq(idempotencyKey(config.getKiosqueroId(), config.getProximaEjecucion()))
        )).thenReturn(new PaymentDetails(
                PAYMENT_ID, null, new BigDecimal(amount), PaymentStatus.REJECTED
        ));
    }

    private void givenPaymentTransferFails(KiosqueroPayoutConfig config, String amount) {
        when(paymentService.transferToThirdParty(
                eq(CVU), eq(CUIT), eq(HOLDER), eq(new BigDecimal(amount)),
                eq(idempotencyKey(config.getKiosqueroId(), config.getProximaEjecucion()))
        )).thenThrow(new RuntimeException("provider unavailable"));
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

    // --- Helpers: When ---

    private void whenExecute() {
        useCase.execute();
    }

    // --- Helpers: Then ---

    private void thenConfigIsSkippedAndNotSaved(KiosqueroPayoutConfig config) {
        verify(repository).findActiveConfigsDueOnOrBefore(TODAY);
        verify(kiosqueroRepository).findBuffetById(KIOSQUERO_ID);
        verify(repository, never()).save(config);
    }

    private void thenNoInteractionsWithPurchaseAndPayment() {
        verifyNoInteractions(purchaseRepository, paymentService);
    }

    private void thenConfigIsMarkedAsExecutedAndSaved(KiosqueroPayoutConfig config) {
        assertEquals(TODAY, config.getUltimaEjecucion());
        assertEquals(TODAY.plusDays(1), config.getProximaEjecucion());
        verify(repository).save(config);
    }

    private void thenPaymentServiceIsNotCalled() {
        verifyNoInteractions(paymentService);
    }

    private void thenConfigIsPausedAndSaved(KiosqueroPayoutConfig config) {
        assertEquals(PayoutScheduleStatus.PAUSED, config.getEstado());
        assertNull(config.getUltimaEjecucion());
        assertEquals(TODAY, config.getProximaEjecucion());
        verify(repository).save(config);
    }

    private void thenConfigIsKeptActiveAndSaved(
            KiosqueroPayoutConfig config, LocalDate expectedLastExecution) {
        assertEquals(PayoutScheduleStatus.ACTIVE, config.getEstado());
        assertEquals(expectedLastExecution, config.getUltimaEjecucion());
        assertEquals(TODAY, config.getProximaEjecucion());
        verify(repository).save(config);
    }
}
