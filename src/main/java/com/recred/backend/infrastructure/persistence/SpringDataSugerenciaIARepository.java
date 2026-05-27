package com.recred.backend.infrastructure.persistence;

import com.recred.backend.infrastructure.entities.SugerenciaIAEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpringDataSugerenciaIARepository extends JpaRepository<SugerenciaIAEntity, UUID> {
    List<SugerenciaIAEntity> findAllByAlumnoEntityIdOrderByCreadoEnDesc(UUID alumnoId);
}
