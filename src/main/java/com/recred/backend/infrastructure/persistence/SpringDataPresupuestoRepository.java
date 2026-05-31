package com.recred.backend.infrastructure.persistence;

import com.recred.backend.infrastructure.entities.StudentBudgetEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataPresupuestoRepository extends
        JpaRepository<StudentBudgetEntity, UUID> {

    Optional<StudentBudgetEntity> findByAlumnoEntityIdAndActivoTrue
            (UUID alumnoId);
}