package com.recred.backend.application;

import com.recred.backend.domain.models.PeriodoPresupuesto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record UpdateBudgetRequest(
        UUID budgetId,
        BigDecimal limitAmount,
        PeriodoPresupuesto period,
        LocalDate startDate,
        List<CategoryRuleRequest> categoryRules
) {
    public record CategoryRuleRequest(UUID categoryId, BigDecimal porcentajeLimite) {}
}
