package com.recred.backend.domain.models;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record SpendingPrediction(
        UUID studentId,
        BudgetPeriod period,
        LocalDate calculationDate,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal currentSpending,
        BigDecimal predictedSpending,
        BigDecimal averageDailySpending,
        BigDecimal limitAmount,
        BigDecimal budgetPercentage,
        BigDecimal confidence,
        int historicalDaysUsed,
        int remainingDays,
        List<ConsumptionCategory> mostConsumedCategories
) {
}
