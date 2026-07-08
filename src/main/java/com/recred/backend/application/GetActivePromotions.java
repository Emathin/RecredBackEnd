package com.recred.backend.application;

import com.recred.backend.domain.models.Promotion;
import com.recred.backend.domain.repositories.PromotionRepository;

import java.util.List;

public class GetActivePromotions {

    private final PromotionRepository promotionRepository;

    public GetActivePromotions(PromotionRepository promotionRepository) {
        this.promotionRepository = promotionRepository;
    }

    public GetActivePromotionsResponse execute(GetActivePromotionsRequest request) {
        return new GetActivePromotionsResponse(promotions().stream()
                .map(CreatePromotionResponse::fromDomain)
                .toList());
    }

    private List<Promotion> promotions() {
        return promotionRepository.findAllActive();
    }
}
