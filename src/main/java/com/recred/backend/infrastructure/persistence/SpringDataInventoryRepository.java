package com.recred.backend.infrastructure.persistence;

import com.recred.backend.infrastructure.entities.InventarioBuffetEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataInventoryRepository extends JpaRepository<InventarioBuffetEntity, UUID> {
    Optional<InventarioBuffetEntity> findByBuffetEntityIdAndProductoEntityId(UUID buffetId, UUID productoId);
}
