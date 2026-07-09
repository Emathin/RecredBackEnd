package com.recred.backend.application;

import com.recred.backend.domain.models.Promotion;
import com.recred.backend.domain.repositories.PromotionRepository;

public class DeletePromotion {

    private final PromotionRepository promotionRepository;

    public DeletePromotion(PromotionRepository promotionRepository) {
        this.promotionRepository = promotionRepository;
    }

    public DeletePromotionResponse execute(DeletePromotionRequest request) {
        Promotion promotion = findPromotion(request.id());
        promotionRepository.deleteById(promotion.getId());
        return new DeletePromotionResponse(promotion.getId());
    }

    private Promotion findPromotion(java.util.UUID id) {
        return promotionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Promotion not found."));
    }
}
