package com.recred.backend.application;

import com.recred.backend.domain.repositories.PromotionRepository;

import java.util.List;

public class GetActivePromotions {

    private final PromotionRepository promotionRepository;

    public GetActivePromotions(PromotionRepository promotionRepository) {
        this.promotionRepository = promotionRepository;
    }

    public List<PromotionResponse> execute() {
        return promotionRepository.findAllActive().stream()
                .map(PromotionResponse::fromDomain)
                .collect(java.util.stream.Collectors.toList());
    }
}
