package com.recred.backend.infrastructure.persistence;

import com.recred.backend.infrastructure.entities.RestriccionHorariaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface SpringDataRestriccionHorariaRepository extends
        JpaRepository<RestriccionHorariaEntity, UUID> {

    List<RestriccionHorariaEntity> findAllByAlumnoEntityIdAndActiva
            (UUID alumnoId, Boolean activa);
    List<RestriccionHorariaEntity> findAllByAlumnoEntityId(UUID alumnoId);

}