package com.recred.backend.infrastructure.persistence;

import com.recred.backend.infrastructure.entities.NivelEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataNivelRepository extends JpaRepository<NivelEntity, UUID> {

    List<NivelEntity> findByActivoTrueOrderByDescripcionAsc();

    Optional<NivelEntity> findByIdAndActivoTrue(UUID id);

    Optional<NivelEntity> findByDescripcionIgnoreCase(String descripcion);
}
