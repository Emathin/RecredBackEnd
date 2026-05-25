package com.recred.backend.infrastructure.persistence;

import com.recred.backend.infrastructure.entities.ProductoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SpringDataProductRepository extends JpaRepository<ProductoEntity, UUID> {
}
