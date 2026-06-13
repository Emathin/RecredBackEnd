package com.recred.backend.domain.models;

import java.math.BigDecimal;

public record PaymentDetails(
    String paymentId,
    String externalReference,
    BigDecimal amount,
    PaymentStatus status
) {
}
