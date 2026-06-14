package com.recred.backend.infrastructure.persistence;

import com.recred.backend.infrastructure.entities.MovimientoStockEntity;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataStockMovementRepository
        extends JpaRepository<MovimientoStockEntity, UUID> {
    List<MovimientoStockEntity> findAllByInventarioIdOrderByCreadoEnDesc(UUID inventarioId);

    @Query("""
            select movement
            from MovimientoStockEntity movement
            where movement.inventario.buffetEntity.id = :buffetId
            and movement.creadoEn between :start and :end
            order by movement.creadoEn asc
            """)
    List<MovimientoStockEntity> findByBuffetAndDateRange(
            @Param("buffetId") UUID buffetId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
}
