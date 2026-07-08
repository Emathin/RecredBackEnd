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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfirmSchoolSubscriptionPaymentTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-07-05T13:00:00Z"),
            ZoneOffset.UTC
    );

    @Mock
    private PagoSuscripcionColegioRepository paymentRepository;

    @Mock
    private ActivateSchoolSubscription activateSchoolSubscription;

    private ConfirmSchoolSubscriptionPayment useCase;

    private UUID schoolId;
    private UUID subscriptionId;
    private PaymentDetails paymentDetails;
    private ConfirmSchoolSubscriptionPaymentResponse response;

    @BeforeEach
    void setUp() {
        useCase = new ConfirmSchoolSubscriptionPayment(
                paymentRepository,
                activateSchoolSubscription,
                CLOCK
        );
    }

    @Test
    void givenApprovedPayment_whenConfirmingPayment_thenActivatesSubscriptionAndSavesPayment() {
        givenSchoolPaymentScenario(PaymentStatus.APPROVED);
        givenPaymentDoesNotExist();
        givenSchoolSubscriptionIsActivated();

        whenConfirmingSchoolSubscriptionPayment();

        thenSubscriptionWasActivated();
        thenPaymentWasApproved();
        thenPaymentWasLinkedToSubscription();
    }

    @Test
    void givenPendingPayment_whenConfirmingPayment_thenSavesPaymentWithoutActivatingSubscription() {
        givenSchoolPaymentScenario(PaymentStatus.PENDING);
        givenPaymentDoesNotExist();

        whenConfirmingSchoolSubscriptionPayment();

        thenSubscriptionWasNotActivated();
        thenPaymentIsPending();
        thenActivationUseCaseWasNotCalled();
    }

    @Test
    void givenProcessedApprovedPayment_whenConfirmingPayment_thenDoesNotActivateAgain() {
        givenSchoolPaymentScenario(PaymentStatus.APPROVED);
        givenPaymentWasAlreadyProcessed();

        whenConfirmingSchoolSubscriptionPayment();

        thenSubscriptionWasNotActivated();
        thenActivationUseCaseWasNotCalled();
        thenPaymentWasNotSaved();
    }

    private void givenSchoolPaymentScenario(PaymentStatus status) {
        schoolId = UUID.randomUUID();
        subscriptionId = UUID.randomUUID();
        paymentDetails = schoolPayment(schoolId, status);
    }

    private void givenPaymentDoesNotExist() {
        when(paymentRepository.findByMercadoPagoPaymentId(paymentDetails.paymentId()))
                .thenReturn(Optional.empty());
    }

    private void givenPaymentWasAlreadyProcessed() {
        when(paymentRepository.findByMercadoPagoPaymentId(paymentDetails.paymentId()))
                .thenReturn(Optional.of(processedPayment()));
    }

    private void givenSchoolSubscriptionIsActivated() {
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

    private void whenConfirmingSchoolSubscriptionPayment() {
        response = useCase.execute(new ConfirmSchoolSubscriptionPaymentRequest(paymentDetails));
    }

    private void thenSubscriptionWasActivated() {
        assertTrue(response.subscriptionActivated());
        verify(activateSchoolSubscription).execute(any(ActivateSchoolSubscriptionRequest.class));
    }

    private void thenSubscriptionWasNotActivated() {
        assertFalse(response.subscriptionActivated());
    }

    private void thenPaymentWasApproved() {
        assertEquals(EstadoPagoExterno.APROBADO, savedPayment().getEstado());
        assertEquals(fixedNow(), savedPayment().getAprobadoEn());
    }

    private void thenPaymentWasLinkedToSubscription() {
        assertEquals(subscriptionId, savedPayment().getSuscripcionId());
    }

    private void thenPaymentIsPending() {
        assertEquals(EstadoPagoExterno.PENDIENTE, savedPayment().getEstado());
    }

    private void thenActivationUseCaseWasNotCalled() {
        verify(activateSchoolSubscription, never()).execute(
                any(ActivateSchoolSubscriptionRequest.class));
    }

    private void thenPaymentWasNotSaved() {
        verify(paymentRepository, never()).save(any(PagoSuscripcionColegio.class));
    }

    private PagoSuscripcionColegio savedPayment() {
        ArgumentCaptor<PagoSuscripcionColegio> captor =
                ArgumentCaptor.forClass(PagoSuscripcionColegio.class);
        verify(paymentRepository).save(captor.capture());
        return captor.getValue();
    }

    private PagoSuscripcionColegio processedPayment() {
        PagoSuscripcionColegio payment =
                PagoSuscripcionColegio.fromPaymentDetails(paymentDetails);
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
        return LocalDateTime.now(CLOCK);
    }
}
