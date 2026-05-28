package com.recred.backend.infrastructure.persistence;

import com.recred.backend.infrastructure.entities.InventarioBuffetEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface SpringDataInventoryRepository extends JpaRepository<InventarioBuffetEntity, UUID> {
    @Query("""
        SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END
        FROM InventarioBuffetEntity i
        WHERE i.productoEntity.id = CAST(:productId AS java.util.UUID)
        AND i.buffetEntity.id = CAST(:buffetId AS java.util.UUID)
""")
    boolean existsByProductIdAndKiosqueroId(UUID productId, UUID buffetId);
}
