package com.recred.backend.application;

import com.recred.backend.domain.models.StudentBudget;
import com.recred.backend.domain.repositories.BudgetRepository;

import java.util.UUID;

public class DisableBudget {

    private final BudgetRepository repository;

    public DisableBudget(BudgetRepository repository) {
        this.repository = repository;
    }

    public void execute(UUID budgetId) {
        StudentBudget presupuesto = repository.findById(budgetId)
                .orElseThrow(() -> new RuntimeException("Presupuesto no " +
                        "encontrado con ID: " + budgetId));

        presupuesto.setActive(false);
        repository.save(presupuesto);
    }
}