package com.recred.backend.application;

import com.recred.backend.domain.models.ProductCategory;
import com.recred.backend.domain.models.StudentBudget;
import com.recred.backend.domain.models.BudgetCategoryRule;
import com.recred.backend.domain.repositories.BudgetRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class UpdateBudgetRules {

    private final BudgetRepository repository;

    public UpdateBudgetRules(BudgetRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public StudentBudget execute(UUID budgetId, UpdateBudgetRulesCommand command) {
        StudentBudget presupuesto = repository.findById(budgetId)
                .orElseThrow(() -> new RuntimeException
                        ("Presupuesto no encontrado con ID: " + budgetId));

        List<BudgetCategoryRule> nuevasReglas = new ArrayList<>();

        if (command.rules() != null && !command.rules().isEmpty()) {
            nuevasReglas = command.rules().stream().map(rule -> {
                ProductCategory categoria = new ProductCategory();
                categoria.setId(rule.categoryId());

                return new BudgetCategoryRule(
                        null,
                        categoria,
                        rule.porcentajeLimite(),
                        null,
                        true
                );
            }).collect(Collectors.toList());
        }

        presupuesto.setCategoryRules(nuevasReglas);

        presupuesto.validatePercentage();
        presupuesto.recalculateRulesBudgets();

        return repository.save(presupuesto);
    }
}