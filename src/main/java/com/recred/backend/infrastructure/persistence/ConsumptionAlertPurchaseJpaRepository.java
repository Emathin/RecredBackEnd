package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.ConsumptionCategoryStatistic;
import com.recred.backend.domain.models.ConsumptionProductStatistic;
import com.recred.backend.domain.models.PurchaseState;
import com.recred.backend.domain.repositories.ConsumptionAlertPurchaseRepository;
import com.recred.backend.infrastructure.entities.PurchaseEntity;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConsumptionAlertPurchaseJpaRepository
        extends JpaRepository<PurchaseEntity, UUID>, ConsumptionAlertPurchaseRepository {

    @Override
    @Query("""
            select count(c.id)
            from PurchaseEntity c
            where c.studentEntity.id = :studentId
            and c.purchaseState in :statuses
            and c.dateAndTime between :startDateTime and :endDateTime
            """)
    long countPurchases(
            @Param("studentId") UUID studentId,
            @Param("statuses") Collection<PurchaseState> statuses,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime
    );

    @Override
    @Query("""
            select new com.recred.backend.domain.models.ConsumptionCategoryStatistic(
                d.productEntity.category.description,
                count(d.id),
                sum(d.amount),
                sum(d.subtotal)
            )
            from PurchaseDetailEntity d
            where d.purchase.studentEntity.id = :studentId
            and d.purchase.purchaseState in :statuses
            and d.purchase.dateAndTime between :startDateTime and :endDateTime
            group by d.productEntity.category.description
            order by sum(d.subtotal) desc
            """)
    List<ConsumptionCategoryStatistic> findCategoryStatistics(
            @Param("studentId") UUID studentId,
            @Param("statuses") Collection<PurchaseState> statuses,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime
    );

    @Override
    @Query("""
            select new com.recred.backend.domain.models.ConsumptionProductStatistic(
                d.productEntity.id,
                d.productEntity.name,
                d.productEntity.category.description,
                count(d.id),
                sum(d.amount),
                sum(d.subtotal)
            )
            from PurchaseDetailEntity d
            where d.purchase.studentEntity.id = :studentId
            and d.purchase.purchaseState in :statuses
            and d.purchase.dateAndTime between :startDateTime and :endDateTime
            group by d.productEntity.id,
                d.productEntity.name,
                d.productEntity.category.description
            order by sum(d.amount) desc, sum(d.subtotal) desc
            """)
    List<ConsumptionProductStatistic> findProductStatistics(
            @Param("studentId") UUID studentId,
            @Param("statuses") Collection<PurchaseState> statuses,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime
    );
}
