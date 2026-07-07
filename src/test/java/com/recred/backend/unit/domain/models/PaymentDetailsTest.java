package com.recred.backend.unit.domain.models;

import com.recred.backend.domain.models.PaymentDetails;
import com.recred.backend.domain.models.PaymentStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentDetailsTest {

    @Test
    void approvedPaymentDetailsKnowTheyWereApproved() {
        PaymentDetails paymentDetails = paymentDetailsWithStatus(PaymentStatus.APPROVED);

        assertTrue(paymentDetails.wasApproved());
    }

    @Test
    void nonApprovedPaymentDetailsKnowTheyWereNotApproved() {
        PaymentDetails paymentDetails = paymentDetailsWithStatus(PaymentStatus.PENDING);

        assertFalse(paymentDetails.wasApproved());
    }

    private PaymentDetails paymentDetailsWithStatus(PaymentStatus status) {
        return new PaymentDetails(
                "payment-id",
                UUID.randomUUID(),
                new BigDecimal("1500.00"),
                status
        );
    }
}
