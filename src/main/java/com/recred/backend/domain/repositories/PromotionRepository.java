package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.Promotion;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Date;

public interface PromotionRepository {
    List<Promotion> findAllActive();
    List<Promotion> findAllActiveByBuffet(UUID buffetId);
    List<Promotion> findAllByBuffet(UUID buffetId);
    List<Promotion> findInactivePromotionsByStartDateBefore(Date date);
    Optional<Promotion> findById(UUID id);
    Promotion save(Promotion promotion);
    void deleteById(UUID id);
}
