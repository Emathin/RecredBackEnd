package com.recred.backend.application;

import com.recred.backend.domain.models.ProductCategory;
import com.recred.backend.domain.models.StudentBudget;
import com.recred.backend.domain.models.BudgetCategoryRule;
import com.recred.backend.domain.repositories.BudgetRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class UpdateBudget {

    private final BudgetRepository repository;

    public UpdateBudget(BudgetRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public StudentBudget execute(UUID budgetId, BudgetCommand command) {
        StudentBudget presupuesto = repository.findById(budgetId)
                .orElseThrow(() -> new RuntimeException
                        ("Presupuesto no encontrado con ID: " + budgetId));

        presupuesto.setLimitAmount(command.limitAmount());
        presupuesto.setPeriod(command.period());
        presupuesto.setStartDate(command.startDate());

        if (command.categoryRules() != null && !command.categoryRules().isEmpty()) {
            List<BudgetCategoryRule> nuevasReglas =
                    command.categoryRules().stream().map(rule -> {
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

            presupuesto.setCategoryRules(nuevasReglas);
        }

        presupuesto.validatePercentage();
        presupuesto.recalculateRulesBudgets();

        return repository.save(presupuesto);
    }
}