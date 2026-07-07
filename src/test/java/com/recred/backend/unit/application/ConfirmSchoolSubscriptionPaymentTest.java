package com.recred.backend.unit.application;

import com.recred.backend.application.ActivateSchoolSubscription;
import com.recred.backend.application.ActivateSchoolSubscriptionRequest;
import com.recred.backend.application.ActivateSchoolSubscriptionResponse;
import com.recred.backend.application.ConfirmSchoolSubscriptionPayment;
import com.recred.backend.application.ConfirmSchoolSubscriptionPaymentRequest;
import com.recred.backend.application.ConfirmSchoolSubscriptionPaymentResponse;
import com.recred.backend.domain.models.EstadoPagoExterno;
import com.recred.backend.domain.models.EstadoSuscripcion;
import com.recred.backend.domain.models.PagoSuscripcionColegio;
import com.recred.backend.domain.models.PaymentDetails;
import com.recred.backend.domain.models.PaymentPurpose;
import com.recred.backend.domain.models.PaymentStatus;
import com.recred.backend.domain.repositories.PagoSuscripcionColegioRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConfirmSchoolSubscriptionPaymentTest {

    private final PagoSuscripcionColegioRepository paymentRepository =
            mock(PagoSuscripcionColegioRepository.class);
    private final ActivateSchoolSubscription activateSchoolSubscription =
            mock(ActivateSchoolSubscription.class);
    private final Clock clock = Clock.fixed(
            Instant.parse("2026-07-05T13:00:00Z"),
            ZoneOffset.UTC
    );
    private final ConfirmSchoolSubscriptionPayment useCase = new ConfirmSchoolSubscriptionPayment(
            paymentRepository,
            activateSchoolSubscription,
            clock
    );

    @Test
    void execute_WhenPaymentIsApproved_ShouldActivateSubscriptionAndSavePayment() {
        UUID schoolId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();
        PaymentDetails details = schoolPayment(schoolId, PaymentStatus.APPROVED);
        givenMissingPayment(details.paymentId());
        givenActivatedSubscription(subscriptionId, schoolId);

        ConfirmSchoolSubscriptionPaymentResponse result = whenExecuting(details);

        PagoSuscripcionColegio savedPayment = capturedSavedPayment();
        assertTrue(result.subscriptionActivated());
        assertEquals(EstadoPagoExterno.APROBADO, savedPayment.getEstado());
        assertEquals(subscriptionId, savedPayment.getSuscripcionId());
        assertEquals(fixedNow(), savedPayment.getAprobadoEn());
        verify(activateSchoolSubscription).execute(any(ActivateSchoolSubscriptionRequest.class));
    }

    @Test
    void execute_WhenPaymentIsPending_ShouldSavePaymentWithoutActivatingSubscription() {
        PaymentDetails details = schoolPayment(UUID.randomUUID(), PaymentStatus.PENDING);
        givenMissingPayment(details.paymentId());

        ConfirmSchoolSubscriptionPaymentResponse result = whenExecuting(details);

        PagoSuscripcionColegio savedPayment = capturedSavedPayment();
        assertFalse(result.subscriptionActivated());
        assertEquals(EstadoPagoExterno.PENDIENTE, savedPayment.getEstado());
        verify(activateSchoolSubscription, never()).execute(
                any(ActivateSchoolSubscriptionRequest.class));
    }

    @Test
    void execute_WhenApprovedPaymentWasAlreadyProcessed_ShouldNotActivateAgain() {
        PaymentDetails details = schoolPayment(UUID.randomUUID(), PaymentStatus.APPROVED);
        PagoSuscripcionColegio payment = processedPayment(details);
        givenExistingPayment(details.paymentId(), payment);

        ConfirmSchoolSubscriptionPaymentResponse result = whenExecuting(details);

        assertFalse(result.subscriptionActivated());
        verify(activateSchoolSubscription, never()).execute(
                any(ActivateSchoolSubscriptionRequest.class));
        verify(paymentRepository, never()).save(any(PagoSuscripcionColegio.class));
    }

    private void givenMissingPayment(String paymentId) {
        when(paymentRepository.findByMercadoPagoPaymentId(paymentId)).thenReturn(Optional.empty());
    }

    private void givenExistingPayment(String paymentId, PagoSuscripcionColegio payment) {
        when(paymentRepository.findByMercadoPagoPaymentId(paymentId))
                .thenReturn(Optional.of(payment));
    }

    private void givenActivatedSubscription(UUID subscriptionId, UUID schoolId) {
        when(activateSchoolSubscription.execute(any(ActivateSchoolSubscriptionRequest.class)))
                .thenReturn(new ActivateSchoolSubscriptionResponse(
                        subscriptionId,
                        schoolId,
                        new BigDecimal("20.00"),
                        "USD",
                        fixedNow(),
                        fixedNow().plusMonths(1),
                        fixedNow().plusMonths(2),
                        EstadoSuscripcion.ACTIVA
                ));
    }

    private ConfirmSchoolSubscriptionPaymentResponse whenExecuting(PaymentDetails details) {
        return useCase.execute(new ConfirmSchoolSubscriptionPaymentRequest(details));
    }

    private PagoSuscripcionColegio capturedSavedPayment() {
        ArgumentCaptor<PagoSuscripcionColegio> captor =
                ArgumentCaptor.forClass(PagoSuscripcionColegio.class);
        verify(paymentRepository).save(captor.capture());
        return captor.getValue();
    }

    private PagoSuscripcionColegio processedPayment(PaymentDetails details) {
        PagoSuscripcionColegio payment = PagoSuscripcionColegio.fromPaymentDetails(details);
        payment.markSubscriptionActivated(UUID.randomUUID(), fixedNow());
        return payment;
    }

    private PaymentDetails schoolPayment(UUID schoolId, PaymentStatus status) {
        return new PaymentDetails(
                "school-payment",
                null,
                new BigDecimal("20.00"),
                status,
                PaymentPurpose.SCHOOL_SUBSCRIPTION,
                null,
                null,
                null,
                schoolId
        );
    }

    private LocalDateTime fixedNow() {
        return LocalDateTime.now(clock);
    }
}
