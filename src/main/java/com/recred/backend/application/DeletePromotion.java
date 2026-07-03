package com.recred.backend.application;

import com.recred.backend.domain.models.Promotion;
import com.recred.backend.domain.models.PromotionStatus;
import com.recred.backend.domain.repositories.PromotionRepository;

public class DeletePromotion {

    private final PromotionRepository promotionRepository;

    public DeletePromotion(PromotionRepository promotionRepository) {
        this.promotionRepository = promotionRepository;
    }

    public DeletePromotionResponse execute(DeletePromotionRequest request) {
        Promotion promotion = promotionRepository.findById(request.id())
                .orElseThrow(() -> new IllegalArgumentException("Promotion not found."));
        promotion.setStatus(PromotionStatus.REJECTED);
        promotionRepository.save(promotion);
        return new DeletePromotionResponse(promotion.getId());
    }
}
