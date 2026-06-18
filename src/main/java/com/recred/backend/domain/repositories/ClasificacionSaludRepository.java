package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.ClasificacionSalud;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClasificacionSaludRepository {
    List<ClasificacionSalud> findAllById(List<UUID> ids);
    Optional<ClasificacionSalud> findById(UUID id);
    List<ClasificacionSalud> findAll();
    Optional<ClasificacionSalud> findByDescripcion(String descripcion);
}