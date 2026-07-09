package com.recred.backend.application;

import com.recred.backend.domain.models.Promotion;
import com.recred.backend.domain.repositories.PromotionRepository;

import java.time.LocalDateTime;

public class TogglePromotionStatus {

    private final PromotionRepository promotionRepository;

    public TogglePromotionStatus(PromotionRepository promotionRepository) {
        this.promotionRepository = promotionRepository;
    }

    public CreatePromotionResponse execute(TogglePromotionStatusRequest request) {
        Promotion promotion = promotionRepository.findById(request.id())
                .orElseThrow(() -> new IllegalArgumentException("Promotion not found."));
        
        promotion.toggleStatus(LocalDateTime.now());
        
        return CreatePromotionResponse.fromDomain(promotionRepository.save(promotion));
    }
}
