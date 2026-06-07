package com.recred.backend.application;

import com.recred.backend.domain.models.PeriodoPresupuesto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record BudgetCommand(
        UUID studentId,
        UUID creadorId,
        BigDecimal limitAmount,
        PeriodoPresupuesto period,
        LocalDate startDate,
        List<CategoryRuleCommand> categoryRules) {
    public record CategoryRuleCommand(UUID categoryId, BigDecimal porcentajeLimite) {
    }
}