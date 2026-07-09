package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.PromotionStatus;
import com.recred.backend.infrastructure.entities.PromotionEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PromotionSpringDataRepository extends JpaRepository<PromotionEntity, UUID> {
    @EntityGraph(attributePaths =
            {"products", "products.categoria", "products.tipos",
                    "products.clasificacionesSalud", "buffetEntity"})
    List<PromotionEntity> findByStatusAndBuffetEntityId(PromotionStatus status, UUID buffetId);

    @EntityGraph(attributePaths =
            {"products", "products.categoria", "products.tipos",
                    "products.clasificacionesSalud", "buffetEntity"})
    List<PromotionEntity> findByBuffetEntityId(UUID buffetId);

    @EntityGraph(attributePaths =
            {"products", "products.categoria", "products.tipos",
                    "products.clasificacionesSalud", "buffetEntity"})
    List<PromotionEntity> findByStatus(PromotionStatus status);

    @EntityGraph(attributePaths =
            {"products", "products.categoria", "products.tipos",
                    "products.clasificacionesSalud", "buffetEntity"})
    List<PromotionEntity> findByStatusAndStartDateBefore(
            PromotionStatus status, LocalDateTime startDate);

    @EntityGraph(attributePaths =
            {"products", "products.categoria", "products.tipos",
                    "products.clasificacionesSalud", "buffetEntity"})
    @Override
    Optional<PromotionEntity> findById(UUID id);
}
