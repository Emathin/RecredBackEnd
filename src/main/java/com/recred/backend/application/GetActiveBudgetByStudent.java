package com.recred.backend.application;

import com.recred.backend.domain.models.PresupuestoAlumno;
import com.recred.backend.domain.repositories.PresupuestoRepository;

import java.util.Optional;
import java.util.UUID;

public class GetActiveBudgetByStudent {

    private final PresupuestoRepository repository;

    public GetActiveBudgetByStudent(PresupuestoRepository repository) {
        this.repository = repository;
    }

    public Optional<PresupuestoAlumno> execute(UUID studentId) {
        return repository.findActivoByAlumnoId(studentId);
    }
}