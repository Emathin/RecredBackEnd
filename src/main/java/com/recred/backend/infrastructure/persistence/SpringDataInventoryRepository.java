package com.recred.backend.infrastructure.persistence;

import com.recred.backend.infrastructure.entities.InventarioBuffetEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataInventoryRepository extends JpaRepository<InventarioBuffetEntity, UUID> {
    Optional<InventarioBuffetEntity> findByBuffetEntityIdAndProductoEntityId(
            UUID buffetId, UUID productoId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT i
            FROM InventarioBuffetEntity i
            WHERE i.buffetEntity.id = :buffetId
            AND i.productoEntity.id = :productoId
            """)
    Optional<InventarioBuffetEntity> findLockedByBuffetEntityIdAndProductoEntityId(
            @Param("buffetId") UUID buffetId,
            @Param("productoId") UUID productoId
    );

    @Query("SELECT i " +
            "FROM InventarioBuffetEntity i " +
            "WHERE i.buffetEntity.id = :buffetId " +
            "AND i.estado IN ('DISPONIBLE', 'BAJO_STOCK') " +
            "AND (i.tipoManejo = 'DISPONIBLE_NO_DISPONIBLE' " +
            "OR i.stockActual > COALESCE(i.stockReservado, 0))")
    List<InventarioBuffetEntity> findAvailableByBuffetId(@Param("buffetId") UUID buffetId);

    @Query("""
        SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END
        FROM InventarioBuffetEntity i
        WHERE i.productoEntity.id = CAST(:productId AS java.util.UUID)
        AND i.buffetEntity.id = CAST(:buffetId AS java.util.UUID)
        AND i.estado IN ('DISPONIBLE', 'BAJO_STOCK')
        AND (i.tipoManejo = 'DISPONIBLE_NO_DISPONIBLE'
            OR i.stockActual > COALESCE(i.stockReservado, 0))
""")
    boolean existsByProductIdAndKiosqueroId(UUID productId, UUID buffetId);

    @Modifying
    @Query("""
            UPDATE InventarioBuffetEntity i
            SET i.urlImagen = :urlImagen
            WHERE i.buffetEntity.id = :buffetId
            AND i.productoEntity.id = :productoId
            """)
    void updateUrlImagen(
            @Param("buffetId") UUID buffetId,
            @Param("productoId") UUID productoId,
            @Param("urlImagen") String urlImagen
    );
}
