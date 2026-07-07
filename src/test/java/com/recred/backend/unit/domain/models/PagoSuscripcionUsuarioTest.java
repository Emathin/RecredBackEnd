package com.recred.backend.unit.domain.models;

import com.recred.backend.domain.models.EstadoPagoExterno;
import com.recred.backend.domain.models.PagoSuscripcionUsuario;
import com.recred.backend.domain.models.PaymentDetails;
import com.recred.backend.domain.models.PaymentStatus;
import com.recred.backend.domain.models.PeriodoSuscripcion;
import com.recred.backend.domain.models.TipoPlan;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PagoSuscripcionUsuarioTest {

    @Test
    void fromPaymentDetails_ShouldCreatePendingPayment() {
        PaymentDetails details = givenSubscriptionPayment(PaymentStatus.PENDING);

        PagoSuscripcionUsuario result = whenCreatingPayment(details);

        assertEquals(details.paymentId(), result.getMercadoPagoPaymentId());
        assertEquals(details.usuarioId(), result.getUsuarioId());
        assertEquals(TipoPlan.INTERMEDIO, result.getPlan());
        assertEquals(PeriodoSuscripcion.MENSUAL, result.getPeriodo());
        assertEquals(EstadoPagoExterno.PENDIENTE, result.getEstado());
        assertFalse(result.shouldActivateSubscription());
    }

    @Test
    void updateFrom_WhenPaymentIsApproved_ShouldMarkPaymentAsApproved() {
        PagoSuscripcionUsuario payment = givenPendingPayment();
        PaymentDetails details = givenSubscriptionPayment(PaymentStatus.APPROVED);

        whenUpdatingPayment(payment, details);

        assertEquals(EstadoPagoExterno.APROBADO, payment.getEstado());
        assertTrue(payment.shouldActivateSubscription());
    }

    @Test
    void markSubscriptionActivated_ShouldStoreSubscriptionReference() {
        PagoSuscripcionUsuario payment = givenPendingPayment();
        payment.updateFrom(givenSubscriptionPayment(PaymentStatus.APPROVED));
        UUID subscriptionId = UUID.randomUUID();
        LocalDateTime approvedAt = LocalDateTime.of(2026, 7, 5, 10, 0);

        whenMarkingActivatedSubscription(payment, subscriptionId, approvedAt);

        assertEquals(subscriptionId, payment.getSuscripcionId());
        assertEquals(approvedAt, payment.getAprobadoEn());
        assertTrue(payment.alreadyActivatedSubscription());
    }

    private PagoSuscripcionUsuario givenPendingPayment() {
        return PagoSuscripcionUsuario.fromPaymentDetails(
                givenSubscriptionPayment(PaymentStatus.PENDING));
    }

    private PaymentDetails givenSubscriptionPayment(PaymentStatus status) {
        return new PaymentDetails(
                "123",
                null,
                new BigDecimal("5500.00"),
                status,
                com.recred.backend.domain.models.PaymentPurpose.USER_SUBSCRIPTION,
                UUID.randomUUID(),
                TipoPlan.INTERMEDIO,
                PeriodoSuscripcion.MENSUAL
        );
    }

    private PagoSuscripcionUsuario whenCreatingPayment(PaymentDetails details) {
        return PagoSuscripcionUsuario.fromPaymentDetails(details);
    }

    private void whenUpdatingPayment(PagoSuscripcionUsuario payment, PaymentDetails details) {
        payment.updateFrom(details);
    }

    private void whenMarkingActivatedSubscription(
            PagoSuscripcionUsuario payment,
            UUID subscriptionId,
            LocalDateTime approvedAt
    ) {
        payment.markSubscriptionActivated(subscriptionId, approvedAt);
    }
}
