package com.recred.backend.application;

import com.recred.backend.domain.models.Promotion;
import com.recred.backend.domain.models.PromotionStatus;
import com.recred.backend.domain.repositories.PromotionRepository;

import java.util.UUID;

public class DeletePromotion {

    private final PromotionRepository promotionRepository;

    public DeletePromotion(PromotionRepository promotionRepository) {
        this.promotionRepository = promotionRepository;
    }

    public void execute(UUID id) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Promotion not found."));
        promotion.setStatus(PromotionStatus.REJECTED);
        promotionRepository.save(promotion);
    }
}
