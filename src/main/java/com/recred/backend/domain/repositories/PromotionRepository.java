package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.Promotion;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PromotionRepository {
    List<Promotion> findAllActive();
    List<Promotion> findAllActiveByBuffet(UUID buffetId);
    List<Promotion> findAllByBuffet(UUID buffetId);
    List<Promotion> findInactivePromotionsByStartDateBefore(LocalDateTime date);
    Optional<Promotion> findById(UUID id);
    Promotion save(Promotion promotion);
    void deleteById(UUID id);
}
