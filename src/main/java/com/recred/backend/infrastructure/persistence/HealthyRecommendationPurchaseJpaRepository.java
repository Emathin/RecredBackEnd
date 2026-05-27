package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.EstadoInventario;
import com.recred.backend.domain.models.HealthyProductOption;
import com.recred.backend.domain.models.RecentProductConsumption;
import com.recred.backend.domain.repositories.HealthyRecommendationPurchaseRepository;
import com.recred.backend.infrastructure.entities.CompraDetalleEntity;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HealthyRecommendationPurchaseJpaRepository
        extends JpaRepository<CompraDetalleEntity, UUID>,
        HealthyRecommendationPurchaseRepository {

    @Override
    @Query("""
            select new com.recred.backend.domain.models.RecentProductConsumption(
                d.productoEntity.id,
                d.productoEntity.nombre,
                d.productoEntity.descripcion,
                d.productoEntity.categoria.descripcion,
                d.compra.buffetEntity.id,
                d.compra.fechaHora,
                d.cantidad
            )
            from CompraDetalleEntity d
            where d.compra.alumnoEntity.id = :studentId
            and d.compra.estado in :statuses
            and d.compra.fechaHora between :startDateTime and :endDateTime
            """)
    List<RecentProductConsumption> findConsumptions(
            @Param("studentId") UUID studentId,
            @Param("statuses") Collection<EstadoCompra> statuses,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime
    );

    @Override
    @Query("""
            select distinct new com.recred.backend.domain.models.HealthyProductOption(
                p.id,
                p.nombre,
                p.descripcion,
                p.categoria.descripcion
            )
            from InventarioBuffetEntity i
            join i.productoEntity p
            where i.buffetEntity.id = :buffetId
            and i.stockActual > 0
            and i.estado in :states
            and p.activo = true
            and p.id <> :excludedProductId
            and not exists (
                select h.id
                from ProductoEntity product
                join product.clasificacionesSalud h
                where product.id = p.id
                and h.id in (
                    select r.id
                    from AlumnoEntity student
                    join student.restriccionesSalud r
                    where student.id = :studentId
                )
            )
            order by p.nombre
            """)
    List<HealthyProductOption> findAvailableOptions(
            @Param("studentId") UUID studentId,
            @Param("buffetId") UUID buffetId,
            @Param("excludedProductId") UUID excludedProductId,
            @Param("states") Collection<EstadoInventario> states
    );
}
