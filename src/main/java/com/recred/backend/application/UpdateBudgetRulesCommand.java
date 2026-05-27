package com.recred.backend.application;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record UpdateBudgetRulesCommand(List<CategoryRuleCommand> rules) {
    public record CategoryRuleCommand(UUID categoryId, BigDecimal porcentajeLimite) {}
}