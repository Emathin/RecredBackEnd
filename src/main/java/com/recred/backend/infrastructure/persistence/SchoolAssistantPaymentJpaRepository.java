package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.SchoolAssistantPayment;
import com.recred.backend.domain.repositories.SchoolAssistantPaymentRepository;
import com.recred.backend.infrastructure.entities.MercadoPagoPaymentEntity;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SchoolAssistantPaymentJpaRepository
        extends JpaRepository<MercadoPagoPaymentEntity, UUID>, SchoolAssistantPaymentRepository {

    @Override
    default List<SchoolAssistantPayment> findRecentByStudentId(UUID studentId, int limit) {
        return findRecentByStudentId(studentId, PageRequest.of(0, limit));
    }

    @Override
    default List<SchoolAssistantPayment> findByStudentIdAndDateRange(
            UUID studentId,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime,
            int limit
    ) {
        return findByStudentIdAndDateRange(
                studentId,
                startDateTime,
                endDateTime,
                PageRequest.of(0, limit)
        );
    }

    @Query("""
            select new com.recred.backend.domain.models.SchoolAssistantPayment(
                payment.amount,
                payment.state,
                payment.createdAt,
                payment.approvedAt
            )
            from MercadoPagoPaymentEntity payment
            where payment.walletEntity.studentEntity.id = :studentId
            order by payment.createdAt desc
            """)
    List<SchoolAssistantPayment> findRecentByStudentId(
            @Param("studentId") UUID studentId,
            Pageable pageable
    );

    @Query("""
            select new com.recred.backend.domain.models.SchoolAssistantPayment(
                payment.amount,
                payment.state,
                payment.createdAt,
                payment.approvedAt
            )
            from MercadoPagoPaymentEntity payment
            where payment.walletEntity.studentEntity.id = :studentId
            and payment.createdAt between :startDateTime and :endDateTime
            order by payment.createdAt desc
            """)
    List<SchoolAssistantPayment> findByStudentIdAndDateRange(
            @Param("studentId") UUID studentId,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime,
            Pageable pageable
    );
}
