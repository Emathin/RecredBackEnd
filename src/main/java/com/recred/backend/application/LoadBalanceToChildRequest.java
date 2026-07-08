package com.recred.backend.application;

import java.math.BigDecimal;
import java.util.UUID;

public record LoadBalanceToChildRequest(
        String cognitoSub,
        UUID studentId,
        BigDecimal amount
) {
    public void validate() {
        if (cognitoSub == null || cognitoSub.isBlank()) {
            throw new IllegalArgumentException("Cognito subject cannot be null or blank");
        }
        if (studentId == null) {
            throw new IllegalArgumentException("studentId cannot be null");
        }
        if (amount == null) {
            throw new IllegalArgumentException("amount cannot be null");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
    }
}
