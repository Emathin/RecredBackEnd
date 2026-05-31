package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.PurchaseState;
import com.recred.backend.domain.models.SchoolAssistantPurchase;
import com.recred.backend.domain.repositories.SchoolAssistantPurchaseRepository;
import com.recred.backend.infrastructure.entities.PurchaseEntity;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface SpringDataPurchaseRepository extends JpaRepository<PurchaseEntity, UUID>,
        SchoolAssistantPurchaseRepository {

    @Override
    default List<SchoolAssistantPurchase> findRecentByStudentId(UUID studentId, int limit) {
        return findRecentByStudentId(studentId, PageRequest.of(0, limit));
    }

    @Query("""
            select new com.recred.backend.domain.models.SchoolAssistantPurchase(
                purchase.totalAmount,
                purchase.purchaseState,
                purchase.paymentMethod,
                purchase.dateAndTime
            )
            from PurchaseEntity purchase
            where purchase.studentEntity.id = :studentId
            order by purchase.dateAndTime desc
            """)
    List<SchoolAssistantPurchase> findRecentByStudentId(
            @Param("studentId") UUID studentId,
            Pageable pageable
    );

    @Query("SELECT c " +
            "FROM PurchaseEntity c " +
            "WHERE c.studentEntity.id = :alumnoId " +
            "ORDER BY c.dateAndTime DESC")
    List<PurchaseEntity> findLastByAlumno(@Param("studentId") UUID alumnoId, Pageable pageable);

    @Query("SELECT COALESCE(SUM(c.totalAmount), 0) FROM PurchaseEntity c " +
            "WHERE c.studentEntity.id = :alumnoId " +
            "AND c.purchaseState = :estadoValido " +
            "AND c.dateAndTime BETWEEN :inicio AND :fin")
    BigDecimal sumarGastosGeneralesEnRango(
            @Param("studentId") UUID alumnoId,
            @Param("estadoValido") PurchaseState estadoValido,
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin
    );

    @Query("SELECT COALESCE(SUM(d.subtotal), 0) FROM PurchaseEntity c " +
            "JOIN c.details d " +
            "JOIN d.productEntity p " +
            "WHERE c.studentEntity.id = :alumnoId " +
            "AND c.purchaseState = :estadoValido " +
            "AND c.dateAndTime BETWEEN :inicio AND :fin " +
            "AND p.category.id = :categoriaId")
    BigDecimal sumarGastosPorCategoriaEnRango(
            @Param("studentId") UUID alumnoId,
            @Param("estadoValido") PurchaseState estadoValido,
            @Param("categoryID") UUID categoriaId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin
    );
}
