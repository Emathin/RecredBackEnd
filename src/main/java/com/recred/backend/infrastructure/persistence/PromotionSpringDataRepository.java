package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.PromotionStatus;
import com.recred.backend.infrastructure.entities.PromotionEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface PromotionSpringDataRepository extends JpaRepository<PromotionEntity, UUID> {
    @EntityGraph(attributePaths =
            {"products", "products.categoria", "products.tipos", "products.clasificacionesSalud"})
    List<PromotionEntity> findByStatus(PromotionStatus status);

    @EntityGraph(attributePaths =
            {"products", "products.categoria", "products.tipos", "products.clasificacionesSalud"})
    List<PromotionEntity> findByStatusAndStartDateBefore(
            PromotionStatus status, LocalDateTime startDate);

}
