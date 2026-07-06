package com.recred.backend.application;

import com.recred.backend.domain.models.PaymentDetails;

public record ConfirmPaymentAndCreditRequest(
        String paymentId,
        PaymentDetails paymentDetails
) {
    public ConfirmPaymentAndCreditRequest(String paymentId) {
        this(paymentId, null);
    }
}
