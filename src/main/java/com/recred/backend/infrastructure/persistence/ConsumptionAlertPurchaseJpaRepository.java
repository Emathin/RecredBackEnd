package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.ConsumptionCategoryStatistic;
import com.recred.backend.domain.models.ConsumptionProductStatistic;
import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.repositories.ConsumptionAlertPurchaseRepository;
import com.recred.backend.infrastructure.entities.CompraEntity;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConsumptionAlertPurchaseJpaRepository
        extends JpaRepository<CompraEntity, UUID>, ConsumptionAlertPurchaseRepository {

    @Override
    @Query("""
            select count(c.id)
            from CompraEntity c
            where c.alumnoEntity.id = :studentId
            and c.estado in :statuses
            and c.fechaHora between :startDateTime and :endDateTime
            """)
    long countPurchases(
            @Param("studentId") UUID studentId,
            @Param("statuses") Collection<EstadoCompra> statuses,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime
    );

    @Override
    @Query("""
            select new com.recred.backend.domain.models.ConsumptionCategoryStatistic(
                d.productoEntity.categoria.descripcion,
                count(d.id),
                sum(d.cantidad),
                sum(d.subtotal)
            )
            from CompraDetalleEntity d
            where d.compra.alumnoEntity.id = :studentId
            and d.compra.estado in :statuses
            and d.compra.fechaHora between :startDateTime and :endDateTime
            group by d.productoEntity.categoria.descripcion
            order by sum(d.subtotal) desc
            """)
    List<ConsumptionCategoryStatistic> findCategoryStatistics(
            @Param("studentId") UUID studentId,
            @Param("statuses") Collection<EstadoCompra> statuses,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime
    );

    @Override
    @Query("""
            select new com.recred.backend.domain.models.ConsumptionProductStatistic(
                d.productoEntity.id,
                d.productoEntity.nombre,
                d.productoEntity.categoria.descripcion,
                count(d.id),
                sum(d.cantidad),
                sum(d.subtotal)
            )
            from CompraDetalleEntity d
            where d.compra.alumnoEntity.id = :studentId
            and d.compra.estado in :statuses
            and d.compra.fechaHora between :startDateTime and :endDateTime
            group by d.productoEntity.id,
                d.productoEntity.nombre,
                d.productoEntity.categoria.descripcion
            order by sum(d.cantidad) desc, sum(d.subtotal) desc
            """)
    List<ConsumptionProductStatistic> findProductStatistics(
            @Param("studentId") UUID studentId,
            @Param("statuses") Collection<EstadoCompra> statuses,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime
    );
}
