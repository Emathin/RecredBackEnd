package com.recred.backend.infrastructure.persistence;

import com.recred.backend.infrastructure.entities.ClasificacionTipoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataClasificacionTipoRepository 
        extends JpaRepository<ClasificacionTipoEntity, UUID> {
    Optional<ClasificacionTipoEntity> findByDescripcion(String descripcion);
}
