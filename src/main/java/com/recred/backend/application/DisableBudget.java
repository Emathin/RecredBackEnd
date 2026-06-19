package com.recred.backend.application;

import com.recred.backend.domain.models.PresupuestoAlumno;
import com.recred.backend.domain.repositories.PresupuestoRepository;

import java.util.UUID;

public class DisableBudget {

    private final PresupuestoRepository repository;

    public DisableBudget(PresupuestoRepository repository) {
        this.repository = repository;
    }

    public void execute(UUID budgetId) {
        PresupuestoAlumno budget = repository.findById(budgetId)
                .orElseThrow(() -> new RuntimeException("Presupuesto no " +
                        "encontrado con ID: " + budgetId));

        budget.setActivo(false);
        repository.save(budget);
    }
}
