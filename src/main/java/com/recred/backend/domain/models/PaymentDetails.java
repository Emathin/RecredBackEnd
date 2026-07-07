package com.recred.backend.domain.models;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentDetails(
    String paymentId,
    UUID studentId,
    BigDecimal amount,
    PaymentStatus status
) {
    public boolean wasApproved() {
        return status == PaymentStatus.APPROVED;
    }
}
