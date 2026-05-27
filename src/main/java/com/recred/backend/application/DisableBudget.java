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
        PresupuestoAlumno presupuesto = repository.findById(budgetId)
                .orElseThrow(() -> new RuntimeException("Presupuesto no " +
                        "encontrado con ID: " + budgetId));

        presupuesto.setActivo(false);
        repository.save(presupuesto);
    }
}