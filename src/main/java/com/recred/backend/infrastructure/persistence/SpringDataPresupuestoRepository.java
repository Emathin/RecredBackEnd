package com.recred.backend.infrastructure.persistence;

import aj.org.objectweb.asm.commons.Remapper;
import com.recred.backend.domain.models.PresupuestoAlumno;
import com.recred.backend.infrastructure.entities.PresupuestoAlumnoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataPresupuestoRepository extends JpaRepository<PresupuestoAlumnoEntity, UUID> {

    Optional<PresupuestoAlumnoEntity> findByAlumnoEntityIdAndActivoTrue(UUID alumnoId);
}