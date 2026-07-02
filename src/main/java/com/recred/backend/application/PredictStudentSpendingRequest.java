package com.recred.backend.application;

import com.recred.backend.domain.models.BudgetPeriod;

import java.util.UUID;

public record PredictStudentSpendingRequest(
        UUID studentId,
        BudgetPeriod period
) {
}
