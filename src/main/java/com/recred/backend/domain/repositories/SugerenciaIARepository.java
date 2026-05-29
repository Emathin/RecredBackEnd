package com.recred.backend.domain.repositories;

import com.recred.backend.infrastructure.entities.SugerenciaIAEntity;

import java.util.List;
import java.util.UUID;

public interface SugerenciaIARepository {
    void guardar(SugerenciaIAEntity sugerencia);

    List<SugerenciaIAEntity> findByAlumnoId(UUID alumnoId);

    java.util.Optional<SugerenciaIAEntity> findById(UUID id);
}
