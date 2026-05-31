package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.PurchaseState;
import com.recred.backend.domain.models.StudentConsumptionProduct;
import com.recred.backend.domain.models.StudentConsumptionPurchase;
import com.recred.backend.domain.repositories.StudentConsumptionRepository;
import com.recred.backend.infrastructure.entities.PurchaseDetailEntity;
import com.recred.backend.infrastructure.entities.PurchaseEntity;
import com.recred.backend.infrastructure.entities.ProductEntity;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StudentConsumptionJpaRepository
        extends JpaRepository<PurchaseEntity, UUID>, StudentConsumptionRepository {

    @Override
    default List<StudentConsumptionPurchase> findPurchasesByStudentIdAndDateRange(
            UUID studentId,
            Collection<PurchaseState> statuses,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime
    ) {
        return findPurchaseEntitiesByStudentIdAndDateRange(
                studentId,
                statuses,
                startDateTime,
                endDateTime
        ).stream()
                .map(this::toPurchase)
                .toList();
    }

    @Query("""
            select distinct purchase
            from PurchaseEntity purchase
            left join fetch purchase.buffetEntity buffet
            left join fetch purchase.details detail
            left join fetch detail.productEntity product
            left join fetch product.category category
            where purchase.studentEntity.id = :studentId
            and purchase.purchaseState in :statuses
            and purchase.dateAndTime between :startDateTime and :endDateTime
            order by purchase.dateAndTime desc
            """)
    List<PurchaseEntity> findPurchaseEntitiesByStudentIdAndDateRange(
            @Param("studentId") UUID studentId,
            @Param("statuses") Collection<PurchaseState> statuses,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime
    );

    private StudentConsumptionPurchase toPurchase(PurchaseEntity purchase) {
        return new StudentConsumptionPurchase(
                purchase.getId(),
                purchase.getDateAndTime(),
                purchase.getPurchaseState(),
                purchase.getPaymentMethod(),
                purchase.getTotalAmount(),
                purchase.getBuffetEntity().getId(),
                purchase.getBuffetEntity().getName(),
                products(purchase)
        );
    }

    private List<StudentConsumptionProduct> products(PurchaseEntity purchase) {
        return purchase.getDetails()
                .stream()
                .sorted(Comparator.comparing(detail -> detail.getProductEntity().getName()))
                .map(this::toProduct)
                .toList();
    }

    private StudentConsumptionProduct toProduct(PurchaseDetailEntity detail) {
        ProductEntity product = detail.getProductEntity();
        return new StudentConsumptionProduct(
                product.getId(),
                product.getName(),
                product.getCategory().getDescription(),
                detail.getAmount(),
                detail.getUnitPrice(),
                detail.getSubtotal()
        );
    }
}
