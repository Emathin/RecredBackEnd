package com.recred.backend.application;

import com.recred.backend.domain.models.ProductCategory;
import com.recred.backend.domain.models.StudentBudget;
import com.recred.backend.domain.models.BudgetCategoryRule;
import com.recred.backend.domain.repositories.BudgetRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

public class CreateBudget {

    private final BudgetRepository repository;

    public CreateBudget(BudgetRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public StudentBudget execute(BudgetCommand command) {
        List<BudgetCategoryRule> reglas = null;

        if (command.categoryRules() != null && !command.categoryRules().isEmpty()) {
            reglas = command.categoryRules().stream().map(rule -> {
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

        StudentBudget presupuesto = new StudentBudget(
                null,
                command.studentId(),
                command.limitAmount(),
                command.period(),
                command.startDate(),
                true,
                reglas
        );

        presupuesto.validatePercentage();
        presupuesto.recalculateRulesBudgets();

        return repository.save(presupuesto);
    }
}