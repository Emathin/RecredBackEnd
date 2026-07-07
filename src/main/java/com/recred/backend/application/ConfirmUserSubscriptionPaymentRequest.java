package com.recred.backend.application;

import com.recred.backend.domain.models.PaymentDetails;

public record ConfirmUserSubscriptionPaymentRequest(PaymentDetails paymentDetails) {
}
