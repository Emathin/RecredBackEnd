package com.recred.backend.domain.models;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentDetails(
    String paymentId,
    String externalReference,
    BigDecimal amount,
    PaymentStatus status
) {
}
