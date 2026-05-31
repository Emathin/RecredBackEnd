package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.PurchaseState;
import com.recred.backend.infrastructure.entities.PurchaseEntity;
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
        extends JpaRepository<PurchaseEntity, UUID>, PurchaseSpendingRepository {

    @Override
    @Query("""
            select new com.recred.backend.domain.models.PurchaseSpending(
                c.dateAndTime,
                c.totalAmount
            )
            from PurchaseEntity c
            where c.studentEntity.id = :studentId
            and c.purchaseState in :statuses
            and c.dateAndTime between :startDateTime and :endDateTime
            """)
    List<PurchaseSpending> findSpending(
            @Param("studentId") UUID studentId,
            @Param("statuses") Collection<PurchaseState> statuses,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime
    );

    @Override
    @Query("""
            select new com.recred.backend.domain.models.ConsumptionCategory(
                d.productEntity.category.description,
                sum(d.subtotal)
            )
            from PurchaseDetailEntity d
            where d.purchase.studentEntity.id = :studentId
            and d.purchase.purchaseState in :statuses
            and d.purchase.dateAndTime between :startDateTime and :endDateTime
            group by d.productEntity.category.description
            order by sum(d.subtotal) desc
            """)
    List<ConsumptionCategory> findMostConsumedCategories(
            @Param("studentId") UUID studentId,
            @Param("statuses") Collection<PurchaseState> statuses,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime
    );
}
