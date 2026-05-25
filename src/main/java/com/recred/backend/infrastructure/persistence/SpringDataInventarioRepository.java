package com.recred.backend.infrastructure.persistence;

import com.recred.backend.infrastructure.entities.InventarioBuffetEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface SpringDataInventarioRepository
        extends JpaRepository<InventarioBuffetEntity, UUID> {
    
    @Query("SELECT i " +
            "FROM InventarioBuffetEntity i " +
            "WHERE i.buffetEntity.id = :buffetId AND i.stockActual > 0 AND i.estado = 'DISPONIBLE'")
    List<InventarioBuffetEntity> findAvailableByBuffetId(@Param("buffetId") UUID buffetId);
}
