package com.recred.backend.application;

import com.recred.backend.domain.models.PaymentDetails;

public record ConfirmSchoolSubscriptionPaymentRequest(PaymentDetails paymentDetails) {
}
