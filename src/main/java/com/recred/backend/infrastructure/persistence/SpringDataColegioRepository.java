package com.recred.backend.infrastructure.persistence;

import com.recred.backend.infrastructure.entities.ColegioEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpringDataColegioRepository extends JpaRepository<ColegioEntity, UUID> {

    @EntityGraph(attributePaths = "nivelEntity")
    List<ColegioEntity> findByActivoTrueOrderByNombreAsc();

    boolean existsByIdAndActivoTrue(UUID id);
}
