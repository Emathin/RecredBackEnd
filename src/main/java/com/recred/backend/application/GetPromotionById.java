package com.recred.backend.application;

import com.recred.backend.domain.models.Promotion;
import com.recred.backend.domain.repositories.PromotionRepository;

public class GetPromotionById {

    private final PromotionRepository promotionRepository;

    public GetPromotionById(PromotionRepository promotionRepository) {
        this.promotionRepository = promotionRepository;
    }

    public CreatePromotionResponse execute(GetPromotionByIdRequest request) {
        return CreatePromotionResponse.fromDomain(promotion(request));
    }

    private Promotion promotion(GetPromotionByIdRequest request) {
        return promotionRepository.findById(request.id())
                .orElseThrow(() -> new IllegalArgumentException("Promotion not found."));
    }
}
