package com.recred.backend.application;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferBalanceBetweenChildrenRequest(
        String cognitoSub,
        UUID fromStudentId,
        UUID toStudentId,
        BigDecimal amount
) {
    public void validate() {
        if (cognitoSub == null || cognitoSub.isBlank()) {
            throw new IllegalArgumentException("Cognito subject cannot be null or blank");
        }
        if (fromStudentId == null) {
            throw new IllegalArgumentException("fromStudentId cannot be null");
        }
        if (toStudentId == null) {
            throw new IllegalArgumentException("toStudentId cannot be null");
        }
        if (amount == null) {
            throw new IllegalArgumentException("amount cannot be null");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        if (fromStudentId.equals(toStudentId)) {
            throw new IllegalArgumentException("Cannot transfer to the same student");
        }
    }
}
