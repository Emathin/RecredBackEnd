package com.recred.backend.domain.models;

import java.math.BigDecimal;

public record SpendingPeriodPreference(
        BudgetPeriod period,
        BigDecimal limitAmount
) {
}
