package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.PresupuestoAlumno;

import java.util.Optional;
import java.util.UUID;

public interface PresupuestoRepository {
    PresupuestoAlumno save(PresupuestoAlumno presupuesto);
    Optional<PresupuestoAlumno> findActivoByAlumnoId(UUID alumnoId);
}
