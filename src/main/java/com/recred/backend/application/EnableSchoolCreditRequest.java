package com.recred.backend.application;

import java.math.BigDecimal;
import java.util.UUID;

public record EnableSchoolCreditRequest(
        UUID tutorId,
        UUID studentId,
        BigDecimal amount,
        Integer installments
) {
    public void validate() {
        if (tutorId == null) {
            throw new IllegalArgumentException("tutorId cannot be null");
        }
        if (studentId == null) {
            throw new IllegalArgumentException("studentId cannot be null");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Credit amount must be greater than zero");
        }
    }
}
