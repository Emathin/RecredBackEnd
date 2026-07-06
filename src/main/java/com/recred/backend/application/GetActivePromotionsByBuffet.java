package com.recred.backend.application;

import com.recred.backend.domain.models.Promotion;
import com.recred.backend.domain.repositories.PromotionRepository;

import java.util.List;

public class GetActivePromotionsByBuffet {

    private final PromotionRepository promotionRepository;

    public GetActivePromotionsByBuffet(PromotionRepository promotionRepository) {
        this.promotionRepository = promotionRepository;
    }

    public GetActivePromotionsByBuffetResponse execute(
            GetActivePromotionsByBuffetRequest request
    ) {
        return new GetActivePromotionsByBuffetResponse(promotions(request).stream()
                .map(CreatePromotionResponse::fromDomain)
                .toList());
    }

    private List<Promotion> promotions(GetActivePromotionsByBuffetRequest request) {
        return promotionRepository.findAllActiveByBuffet(request.buffetId());
    }
}
