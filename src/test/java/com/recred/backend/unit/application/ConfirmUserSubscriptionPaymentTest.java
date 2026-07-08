package com.recred.backend.unit.application;

import com.recred.backend.application.ActivateUserSubscription;
import com.recred.backend.application.ActivateUserSubscriptionRequest;
import com.recred.backend.application.ActivateUserSubscriptionResponse;
import com.recred.backend.application.ConfirmUserSubscriptionPayment;
import com.recred.backend.application.ConfirmUserSubscriptionPaymentRequest;
import com.recred.backend.application.ConfirmUserSubscriptionPaymentResponse;
import com.recred.backend.domain.models.EstadoPagoExterno;
import com.recred.backend.domain.models.EstadoSuscripcion;
import com.recred.backend.domain.models.PagoSuscripcionUsuario;
import com.recred.backend.domain.models.PaymentDetails;
import com.recred.backend.domain.models.PaymentPurpose;
import com.recred.backend.domain.models.PaymentStatus;
import com.recred.backend.domain.models.PeriodoSuscripcion;
import com.recred.backend.domain.models.TipoPlan;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.PagoSuscripcionUsuarioRepository;
import com.recred.backend.domain.repositories.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
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
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfirmUserSubscriptionPaymentTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-07-05T13:00:00Z"),
            ZoneOffset.UTC
    );

    @Mock
    private PagoSuscripcionUsuarioRepository paymentRepository;

    @Mock
    private UsuarioRepository userRepository;

    @Mock
    private ActivateUserSubscription activateUserSubscription;

    private ConfirmUserSubscriptionPayment useCase;

    private UUID userId;
    private UUID subscriptionId;
    private PaymentDetails paymentDetails;
    private ConfirmUserSubscriptionPaymentResponse response;

    @BeforeEach
    void setUp() {
        useCase = new ConfirmUserSubscriptionPayment(
                paymentRepository,
                userRepository,
                activateUserSubscription,
                CLOCK
        );
    }

    @Test
    void givenApprovedPayment_whenConfirmingPayment_thenActivatesSubscriptionAndSavesPayment() {
        givenUserPaymentScenario(PaymentStatus.APPROVED);
        givenSubscriptionOwnerExists();
        givenPaymentDoesNotExist();
        givenPaymentCanBeSaved();
        givenUserSubscriptionIsActivated();

        whenConfirmingUserSubscriptionPayment();

        thenSubscriptionWasActivated();
        thenPaymentWasApproved();
        thenPaymentWasLinkedToSubscription();
        thenPaymentWasSavedBeforeActivatingSubscription();
    }

    @Test
    void givenPendingPayment_whenConfirmingPayment_thenSavesPaymentWithoutActivatingSubscription() {
        givenUserPaymentScenario(PaymentStatus.PENDING);
        givenSubscriptionOwnerExists();
        givenPaymentDoesNotExist();
        givenPaymentCanBeSaved();

        whenConfirmingUserSubscriptionPayment();

        thenSubscriptionWasNotActivated();
        thenPaymentIsPending();
        thenActivationUseCaseWasNotCalled();
    }

    @Test
    void givenProcessedApprovedPayment_whenConfirmingPayment_thenDoesNotActivateAgain() {
        givenUserPaymentScenario(PaymentStatus.APPROVED);
        givenSubscriptionOwnerExists();
        givenPaymentWasAlreadyProcessed();

        whenConfirmingUserSubscriptionPayment();

        thenSubscriptionWasNotActivated();
        thenActivationUseCaseWasNotCalled();
        thenPaymentWasNotSaved();
    }

    private void givenUserPaymentScenario(PaymentStatus status) {
        userId = UUID.randomUUID();
        subscriptionId = UUID.randomUUID();
        paymentDetails = subscriptionPayment(userId, status);
    }

    private void givenSubscriptionOwnerExists() {
        when(userRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(user(userId)));
    }

    private void givenPaymentDoesNotExist() {
        when(paymentRepository.findByMercadoPagoPaymentId(paymentDetails.paymentId()))
                .thenReturn(Optional.empty());
    }

    private void givenPaymentWasAlreadyProcessed() {
        when(paymentRepository.findByMercadoPagoPaymentId(paymentDetails.paymentId()))
                .thenReturn(Optional.of(processedPayment()));
    }

    private void givenPaymentCanBeSaved() {
        when(paymentRepository.save(any(PagoSuscripcionUsuario.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void givenUserSubscriptionIsActivated() {
        when(activateUserSubscription.execute(any(ActivateUserSubscriptionRequest.class)))
                .thenReturn(new ActivateUserSubscriptionResponse(
                        subscriptionId,
                        userId,
                        TipoPlan.INTERMEDIO,
                        PeriodoSuscripcion.MENSUAL,
                        new BigDecimal("5500.00"),
                        fixedNow(),
                        fixedNow().plusMonths(1),
                        EstadoSuscripcion.ACTIVA
                ));
    }

    private void whenConfirmingUserSubscriptionPayment() {
        response = useCase.execute(new ConfirmUserSubscriptionPaymentRequest(paymentDetails));
    }

    private void thenSubscriptionWasActivated() {
        assertTrue(response.subscriptionActivated());
        verify(activateUserSubscription).execute(any(ActivateUserSubscriptionRequest.class));
    }

    private void thenSubscriptionWasNotActivated() {
        assertFalse(response.subscriptionActivated());
    }

    private void thenPaymentWasApproved() {
        assertEquals(EstadoPagoExterno.APROBADO, lastSavedPayment().getEstado());
        assertEquals(fixedNow(), lastSavedPayment().getAprobadoEn());
    }

    private void thenPaymentWasLinkedToSubscription() {
        assertEquals(subscriptionId, lastSavedPayment().getSuscripcionId());
    }

    private void thenPaymentIsPending() {
        assertEquals(EstadoPagoExterno.PENDIENTE, lastSavedPayment().getEstado());
    }

    private void thenActivationUseCaseWasNotCalled() {
        verify(activateUserSubscription, never()).execute(
                any(ActivateUserSubscriptionRequest.class));
    }

    private void thenPaymentWasNotSaved() {
        verify(paymentRepository, never()).save(any(PagoSuscripcionUsuario.class));
    }

    private void thenPaymentWasSavedBeforeActivatingSubscription() {
        InOrder inOrder = inOrder(paymentRepository, activateUserSubscription);
        inOrder.verify(paymentRepository).save(any(PagoSuscripcionUsuario.class));
        inOrder.verify(activateUserSubscription).execute(
                any(ActivateUserSubscriptionRequest.class));
        inOrder.verify(paymentRepository).save(any(PagoSuscripcionUsuario.class));
    }

    private PagoSuscripcionUsuario lastSavedPayment() {
        ArgumentCaptor<PagoSuscripcionUsuario> captor =
                ArgumentCaptor.forClass(PagoSuscripcionUsuario.class);
        verify(paymentRepository, atLeastOnce()).save(captor.capture());
        return captor.getAllValues().get(captor.getAllValues().size() - 1);
    }

    private PagoSuscripcionUsuario processedPayment() {
        PagoSuscripcionUsuario payment = PagoSuscripcionUsuario.fromPaymentDetails(paymentDetails);
        payment.markSubscriptionActivated(UUID.randomUUID(), fixedNow());
        return payment;
    }

    private PaymentDetails subscriptionPayment(UUID userId, PaymentStatus status) {
        return new PaymentDetails(
                "mp-123",
                null,
                new BigDecimal("5500.00"),
                status,
                PaymentPurpose.USER_SUBSCRIPTION,
                userId,
                TipoPlan.INTERMEDIO,
                PeriodoSuscripcion.MENSUAL
        );
    }

    private Usuario user(UUID userId) {
        Usuario user = new Usuario();
        user.setId(userId);
        return user;
    }

    private LocalDateTime fixedNow() {
        return LocalDateTime.now(CLOCK);
    }
}
