package com.recred.backend.unit.domain.models;

import com.recred.backend.domain.models.EstadoPagoExterno;
import com.recred.backend.domain.models.PagoSuscripcionColegio;
import com.recred.backend.domain.models.PaymentDetails;
import com.recred.backend.domain.models.PaymentPurpose;
import com.recred.backend.domain.models.PaymentStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PagoSuscripcionColegioTest {

    @Test
    void fromPaymentDetails_ShouldCreatePendingPayment() {
        PaymentDetails details = givenSchoolPayment(PaymentStatus.PENDING);

        PagoSuscripcionColegio result = whenCreatingPayment(details);

        assertEquals(details.paymentId(), result.getMercadoPagoPaymentId());
        assertEquals(details.colegioId(), result.getColegioId());
        assertEquals(EstadoPagoExterno.PENDIENTE, result.getEstado());
        assertEquals("USD", result.getMoneda());
        assertFalse(result.shouldActivateSubscription());
    }

    @Test
    void updateFrom_WhenPaymentIsApproved_ShouldMarkPaymentAsApproved() {
        PagoSuscripcionColegio payment = givenPendingPayment();
        PaymentDetails details = givenSchoolPayment(PaymentStatus.APPROVED);

        whenUpdatingPayment(payment, details);

        assertEquals(EstadoPagoExterno.APROBADO, payment.getEstado());
        assertTrue(payment.shouldActivateSubscription());
    }

    @Test
    void markSubscriptionActivated_ShouldStoreSubscriptionReference() {
        PagoSuscripcionColegio payment = givenPendingPayment();
        payment.updateFrom(givenSchoolPayment(PaymentStatus.APPROVED));
        UUID subscriptionId = UUID.randomUUID();
        LocalDateTime approvedAt = LocalDateTime.of(2026, 7, 5, 10, 0);

        whenMarkingActivatedSubscription(payment, subscriptionId, approvedAt);

        assertEquals(subscriptionId, payment.getSuscripcionId());
        assertEquals(approvedAt, payment.getAprobadoEn());
        assertTrue(payment.alreadyActivatedSubscription());
    }

    private PagoSuscripcionColegio givenPendingPayment() {
        return PagoSuscripcionColegio.fromPaymentDetails(givenSchoolPayment(PaymentStatus.PENDING));
    }

    private PaymentDetails givenSchoolPayment(PaymentStatus status) {
        return new PaymentDetails(
                "school-payment",
                null,
                new BigDecimal("20.00"),
                status,
                PaymentPurpose.SCHOOL_SUBSCRIPTION,
                null,
                null,
                null,
                UUID.randomUUID()
        );
    }

    private PagoSuscripcionColegio whenCreatingPayment(PaymentDetails details) {
        return PagoSuscripcionColegio.fromPaymentDetails(details);
    }

    private void whenUpdatingPayment(PagoSuscripcionColegio payment, PaymentDetails details) {
        payment.updateFrom(details);
    }

    private void whenMarkingActivatedSubscription(
            PagoSuscripcionColegio payment,
            UUID subscriptionId,
            LocalDateTime approvedAt
    ) {
        payment.markSubscriptionActivated(subscriptionId, approvedAt);
    }
}
