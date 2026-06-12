package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.PromotionStatus;
import com.recred.backend.infrastructure.entities.PromotionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PromotionSpringDataRepository extends JpaRepository<PromotionEntity, UUID> {
    List<PromotionEntity> findByStatus(PromotionStatus status);
}
