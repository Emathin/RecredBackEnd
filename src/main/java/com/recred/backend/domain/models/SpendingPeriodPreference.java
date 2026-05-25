package com.recred.backend.domain.models;

import com.recred.backend.domain.models.BudgetPeriod;
import java.math.BigDecimal;

public record SpendingPeriodPreference(
        BudgetPeriod period,
        BigDecimal limitAmount
) {
}
