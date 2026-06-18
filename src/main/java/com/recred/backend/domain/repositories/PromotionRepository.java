package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.Promotion;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PromotionRepository {
    List<Promotion> findAllActive();
    List<Promotion> findDraftPromotionsByStartDateBefore(java.time.LocalDateTime date);
    Optional<Promotion> findById(UUID id);
    Promotion save(Promotion promotion);
}
