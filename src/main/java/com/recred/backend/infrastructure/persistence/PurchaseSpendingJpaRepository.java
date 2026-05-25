package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.infrastructure.entities.CompraEntity;
import com.recred.backend.domain.models.ConsumptionCategory;
import com.recred.backend.domain.models.PurchaseSpending;
import com.recred.backend.domain.repositories.PurchaseSpendingRepository;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PurchaseSpendingJpaRepository
        extends JpaRepository<CompraEntity, UUID>, PurchaseSpendingRepository {

    @Override
    @Query("""
            select new com.recred.backend.domain.models.PurchaseSpending(
                c.fechaHora,
                c.montoTotal
            )
            from CompraEntity c
            where c.alumnoEntity.id = :studentId
            and c.estado in :statuses
            and c.fechaHora between :startDateTime and :endDateTime
            """)
    List<PurchaseSpending> findSpending(
            @Param("studentId") UUID studentId,
            @Param("statuses") Collection<EstadoCompra> statuses,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime
    );

    @Override
    @Query("""
            select new com.recred.backend.domain.models.ConsumptionCategory(
                d.productoEntity.categoria.descripcion,
                sum(d.subtotal)
            )
            from CompraDetalleEntity d
            where d.compra.alumnoEntity.id = :studentId
            and d.compra.estado in :statuses
            and d.compra.fechaHora between :startDateTime and :endDateTime
            group by d.productoEntity.categoria.descripcion
            order by sum(d.subtotal) desc
            """)
    List<ConsumptionCategory> findMostConsumedCategories(
            @Param("studentId") UUID studentId,
            @Param("statuses") Collection<EstadoCompra> statuses,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime
    );
}
