package com.recred.backend.infrastructure.persistence;

import com.recred.backend.infrastructure.entities.InventarioBuffetEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataInventoryRepository extends JpaRepository<InventarioBuffetEntity, UUID> {
    Optional<InventarioBuffetEntity> findByBuffetEntityIdAndProductoEntityId(
            UUID buffetId, UUID productoId);

    @Query("SELECT i " +
            "FROM InventarioBuffetEntity i " +
            "WHERE i.buffetEntity.id = :buffetId AND i.stockActual > 0 AND i.estado = 'DISPONIBLE'")
    List<InventarioBuffetEntity> findAvailableByBuffetId(@Param("buffetId") UUID buffetId);

    @Query("""
        SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END
        FROM InventarioBuffetEntity i
        WHERE i.productoEntity.id = CAST(:productId AS java.util.UUID)
        AND i.buffetEntity.id = CAST(:buffetId AS java.util.UUID)
""")
    boolean existsByProductIdAndKiosqueroId(UUID productId, UUID buffetId);
}
