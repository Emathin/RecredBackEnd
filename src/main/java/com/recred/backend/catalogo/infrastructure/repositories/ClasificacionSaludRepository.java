package com.recred.backend.catalogo.infrastructure.repositories;

import com.recred.backend.catalogo.infrastructure.entities.ClasificacionSaludEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ClasificacionSaludRepository
		extends JpaRepository<ClasificacionSaludEntity, UUID> {
}