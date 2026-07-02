package com.recred.backend.infrastructure.persistence;

import com.recred.backend.infrastructure.entities.ProveedorEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataProveedorRepository extends JpaRepository<ProveedorEntity, UUID> {
    List<ProveedorEntity> findByKiosqueroId(UUID kiosqueroId);
    Optional<ProveedorEntity> findByIdAndKiosqueroId(UUID id, UUID kiosqueroId);
    boolean existsByIdAndKiosqueroId(UUID id, UUID kiosqueroId);
}
