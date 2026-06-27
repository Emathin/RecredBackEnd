package com.recred.backend.application;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record UpdateBudgetRulesRequest(
        UUID budgetId,
        List<CategoryRuleRequest> rules
) {
    public record CategoryRuleRequest(UUID categoryId, BigDecimal porcentajeLimite) {}
}
