package com.recred.backend.application;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ScheduleWalletTopUpRequest(
        UUID studentId,
        BigDecimal amount,
        Integer frequency,
        String frequencyType,
        OffsetDateTime startDate
) {
    public ScheduleWalletTopUpRequest(
            UUID studentId,
            BigDecimal amount,
            Integer frequency,
            String frequencyType) {
        this(studentId, amount, frequency, frequencyType, null);
    }

    public void validate() {
        if (studentId == null) {
            throw new IllegalArgumentException("studentId cannot be null");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be greater than zero");
        }
        if (frequency == null || frequency <= 0) {
            throw new IllegalArgumentException("frequency must be greater than zero");
        }
        if (frequencyType == null || frequencyType.isBlank()) {
            throw new IllegalArgumentException("frequencyType cannot be null or empty");
        }
    }
}
