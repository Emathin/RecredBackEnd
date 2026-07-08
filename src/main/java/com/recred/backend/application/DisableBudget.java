package com.recred.backend.application;

import com.recred.backend.domain.models.PresupuestoAlumno;
import com.recred.backend.domain.repositories.PresupuestoRepository;

import java.util.UUID;

public class DisableBudget {

    private final PresupuestoRepository repository;

    public DisableBudget(PresupuestoRepository repository) {
        this.repository = repository;
    }

    public DisableBudgetResponse execute(DisableBudgetRequest disableBudgetRequest) {
        PresupuestoAlumno budget = findBudgetById(disableBudgetRequest.budgetId());
        disableAndSaveBudget(budget);
        return new DisableBudgetResponse();
    }

    private PresupuestoAlumno findBudgetById(UUID budgetId) {
        return repository.findById(budgetId)
                .orElseThrow(() -> new RuntimeException(
                        "Presupuesto no encontrado con ID: " + budgetId));
    }

    private void disableAndSaveBudget(PresupuestoAlumno budget) {
        budget.setActivo(false);
        repository.save(budget);
    }
}
