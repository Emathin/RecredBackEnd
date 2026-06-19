package com.recred.backend.infrastructure.persistence;

import com.recred.backend.infrastructure.entities.ClasificacionSaludEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataClasificacionSaludRepository 
        extends JpaRepository<ClasificacionSaludEntity, UUID> {
    Optional<ClasificacionSaludEntity> findByDescripcion(String descripcion);
}