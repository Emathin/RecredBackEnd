package com.recred.backend.infrastructure.persistence;

import com.recred.backend.infrastructure.entities.CategoriaProductoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataCategoryRepository extends JpaRepository<CategoriaProductoEntity, UUID> {
    Optional<CategoriaProductoEntity> findByDescripcion(String descripcion);
}
