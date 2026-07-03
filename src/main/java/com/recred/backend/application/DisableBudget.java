package com.recred.backend.application;

import com.recred.backend.domain.models.PresupuestoAlumno;
import com.recred.backend.domain.repositories.PresupuestoRepository;

import java.util.UUID;

public class DisableBudget {

    private final PresupuestoRepository repository;

    public DisableBudget(PresupuestoRepository repository) {
        this.repository = repository;
    }

    public DisableBudgetResponse execute(DisableBudgetRequest request) {
        PresupuestoAlumno presupuesto = this.findPresupuestoById(request.budgetId());
        presupuesto.setActivo(false);
        repository.save(presupuesto);
        return new DisableBudgetResponse();
    }

    private PresupuestoAlumno findPresupuestoById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException
                        ("Presupuesto no encontrado con ID: " + id));
    }
}
