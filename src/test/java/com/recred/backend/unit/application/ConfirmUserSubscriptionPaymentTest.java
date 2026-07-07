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
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConfirmUserSubscriptionPaymentTest {

    private final PagoSuscripcionUsuarioRepository paymentRepository =
            mock(PagoSuscripcionUsuarioRepository.class);
    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final ActivateUserSubscription activateUserSubscription =
            mock(ActivateUserSubscription.class);
    private final Clock clock = Clock.fixed(
            Instant.parse("2026-07-05T13:00:00Z"),
            ZoneOffset.UTC
    );
    private final ConfirmUserSubscriptionPayment useCase = new ConfirmUserSubscriptionPayment(
            paymentRepository,
            usuarioRepository,
            activateUserSubscription,
            clock
    );

    @BeforeEach
    void setUp() {
        givenPaymentCanBeSaved();
    }

    @Test
    void execute_WhenPaymentIsApproved_ShouldActivateSubscriptionAndSavePayment() {
        UUID userId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();
        PaymentDetails details = subscriptionPayment(userId, PaymentStatus.APPROVED);
        givenSubscriptionOwner(userId);
        givenMissingPayment(details.paymentId());
        givenActivatedSubscription(subscriptionId);

        ConfirmUserSubscriptionPaymentResponse result = whenExecuting(details);

        PagoSuscripcionUsuario savedPayment = lastSavedPayment();
        assertTrue(result.subscriptionActivated());
        assertEquals(EstadoPagoExterno.APROBADO, savedPayment.getEstado());
        assertEquals(subscriptionId, savedPayment.getSuscripcionId());
        assertEquals(fixedNow(), savedPayment.getAprobadoEn());
        verifyPaymentWasSavedBeforeActivatingSubscription();
        verify(activateUserSubscription).execute(any(ActivateUserSubscriptionRequest.class));
    }

    @Test
    void execute_WhenPaymentIsPending_ShouldSavePaymentWithoutActivatingSubscription() {
        UUID userId = UUID.randomUUID();
        PaymentDetails details = subscriptionPayment(userId, PaymentStatus.PENDING);
        givenSubscriptionOwner(userId);
        givenMissingPayment(details.paymentId());

        ConfirmUserSubscriptionPaymentResponse result = whenExecuting(details);

        PagoSuscripcionUsuario savedPayment = lastSavedPayment();
        assertFalse(result.subscriptionActivated());
        assertEquals(EstadoPagoExterno.PENDIENTE, savedPayment.getEstado());
        verify(activateUserSubscription, never()).execute(
                any(ActivateUserSubscriptionRequest.class));
    }

    @Test
    void execute_WhenApprovedPaymentWasAlreadyProcessed_ShouldNotActivateAgain() {
        UUID userId = UUID.randomUUID();
        PaymentDetails details = subscriptionPayment(userId, PaymentStatus.APPROVED);
        PagoSuscripcionUsuario payment = processedPayment(details);
        givenSubscriptionOwner(userId);
        givenExistingPayment(details.paymentId(), payment);

        ConfirmUserSubscriptionPaymentResponse result = whenExecuting(details);

        assertFalse(result.subscriptionActivated());
        verify(activateUserSubscription, never()).execute(
                any(ActivateUserSubscriptionRequest.class));
        verify(paymentRepository, never()).save(any(PagoSuscripcionUsuario.class));
    }

    private void givenSubscriptionOwner(UUID userId) {
        when(usuarioRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(user(userId)));
    }

    private void givenMissingPayment(String paymentId) {
        when(paymentRepository.findByMercadoPagoPaymentId(paymentId)).thenReturn(Optional.empty());
    }

    private void givenExistingPayment(String paymentId, PagoSuscripcionUsuario payment) {
        when(paymentRepository.findByMercadoPagoPaymentId(paymentId))
                .thenReturn(Optional.of(payment));
    }

    private void givenActivatedSubscription(UUID subscriptionId) {
        when(activateUserSubscription.execute(any(ActivateUserSubscriptionRequest.class)))
                .thenReturn(new ActivateUserSubscriptionResponse(
                        subscriptionId,
                        UUID.randomUUID(),
                        TipoPlan.INTERMEDIO,
                        PeriodoSuscripcion.MENSUAL,
                        new BigDecimal("5500.00"),
                        fixedNow(),
                        fixedNow().plusMonths(1),
                        EstadoSuscripcion.ACTIVA
                ));
    }

    private void givenPaymentCanBeSaved() {
        when(paymentRepository.save(any(PagoSuscripcionUsuario.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private ConfirmUserSubscriptionPaymentResponse whenExecuting(PaymentDetails details) {
        return useCase.execute(new ConfirmUserSubscriptionPaymentRequest(details));
    }

    private PagoSuscripcionUsuario lastSavedPayment() {
        ArgumentCaptor<PagoSuscripcionUsuario> captor =
                ArgumentCaptor.forClass(PagoSuscripcionUsuario.class);
        verify(paymentRepository, atLeastOnce()).save(captor.capture());
        return captor.getAllValues().get(captor.getAllValues().size() - 1);
    }

    private void verifyPaymentWasSavedBeforeActivatingSubscription() {
        InOrder inOrder = inOrder(paymentRepository, activateUserSubscription);
        inOrder.verify(paymentRepository).save(any(PagoSuscripcionUsuario.class));
        inOrder.verify(activateUserSubscription).execute(
                any(ActivateUserSubscriptionRequest.class));
        inOrder.verify(paymentRepository).save(any(PagoSuscripcionUsuario.class));
    }

    private PagoSuscripcionUsuario processedPayment(PaymentDetails details) {
        PagoSuscripcionUsuario payment = PagoSuscripcionUsuario.fromPaymentDetails(details);
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
        return LocalDateTime.now(clock);
    }
}
