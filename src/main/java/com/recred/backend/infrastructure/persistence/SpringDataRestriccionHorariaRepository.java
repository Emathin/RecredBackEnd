package com.recred.backend.infrastructure.persistence;

import com.recred.backend.infrastructure.entities.TimeRestrictionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface SpringDataRestriccionHorariaRepository extends
        JpaRepository<TimeRestrictionEntity, UUID> {

    List<TimeRestrictionEntity> findAllByAlumnoEntityIdAndActiva
            (UUID alumnoId, Boolean activa);
    List<TimeRestrictionEntity> findAllByAlumnoEntityId(UUID alumnoId);

}