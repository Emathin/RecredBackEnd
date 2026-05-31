package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.PurchaseState;
import com.recred.backend.domain.models.InventoryState;
import com.recred.backend.domain.models.HealthyProductOption;
import com.recred.backend.domain.models.RecentProductConsumption;
import com.recred.backend.domain.repositories.HealthyRecommendationPurchaseRepository;
import com.recred.backend.infrastructure.entities.PurchaseDetailEntity;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HealthyRecommendationPurchaseJpaRepository
        extends JpaRepository<PurchaseDetailEntity, UUID>,
        HealthyRecommendationPurchaseRepository {

    @Override
    @Query("""
            select new com.recred.backend.domain.models.RecentProductConsumption(
                d.productEntity.id,
                d.productEntity.name,
                d.productEntity.description,
                d.productEntity.category.description,
                d.purchase.buffetEntity.id,
                d.purchase.dateAndTime,
                d.amount
            )
            from PurchaseDetailEntity d
            where d.purchase.studentEntity.id = :studentId
            and d.purchase.purchaseState in :statuses
            and d.purchase.dateAndTime between :startDateTime and :endDateTime
            """)
    List<RecentProductConsumption> findConsumptions(
            @Param("studentId") UUID studentId,
            @Param("statuses") Collection<PurchaseState> statuses,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime
    );

    @Override
    @Query("""
            select distinct new com.recred.backend.domain.models.HealthyProductOption(
                p.id,
                p.name,
                p.description,
                p.category.description
            )
            from BuffetInventoryEntity i
            join i.productEntity p
            where i.buffetEntity.id = :buffetId
            and i.currentStock > 0
            and i.state in :states
            and p.active = true
            and p.id <> :excludedProductId
            and not exists (
                select h.id
                from ProductEntity product
                join product.healthClassifications h
                where product.id = p.id
                and h.id in (
                    select r.id
                    from StudentEntity student
                    join student.healthRestrictions r
                    where student.id = :studentId
                )
            )
            order by p.name
            """)
    List<HealthyProductOption> findAvailableOptions(
            @Param("studentId") UUID studentId,
            @Param("buffetId") UUID buffetId,
            @Param("excludedProductId") UUID excludedProductId,
            @Param("states") Collection<InventoryState> states
    );
}
