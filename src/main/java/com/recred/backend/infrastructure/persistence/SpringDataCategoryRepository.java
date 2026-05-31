package com.recred.backend.infrastructure.persistence;

import com.recred.backend.infrastructure.entities.ProductCategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataCategoryRepository extends JpaRepository<ProductCategoryEntity, UUID> {
    Optional<ProductCategoryEntity> findByDescripcion(String descripcion);
}
