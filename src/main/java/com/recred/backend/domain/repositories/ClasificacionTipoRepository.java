package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.ClasificacionTipo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClasificacionTipoRepository {
    List<ClasificacionTipo> findAllById(List<UUID> ids);
    Optional<ClasificacionTipo> findById(UUID id);
    List<ClasificacionTipo> findAll();
    Optional<ClasificacionTipo> findByDescripcion(String descripcion);
}
