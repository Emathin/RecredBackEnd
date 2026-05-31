package com.recred.backend.application;

import com.recred.backend.domain.models.PeriodBudget;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record BudgetCommand(
        UUID studentId,
        BigDecimal limitAmount,
        PeriodBudget period,
        LocalDate startDate,
        List<CategoryRuleCommand> categoryRules
) {
    public record CategoryRuleCommand(UUID categoryId, BigDecimal porcentajeLimite) {}
}