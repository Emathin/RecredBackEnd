package com.recred.backend.application;

import com.recred.backend.domain.repositories.PromotionRepository;

import java.util.UUID;

public class GetPromotionById {

    private final PromotionRepository promotionRepository;

    public GetPromotionById(PromotionRepository promotionRepository) {
        this.promotionRepository = promotionRepository;
    }

    public PromotionResponse execute(UUID id) {
        return promotionRepository.findById(id)
                .map(PromotionResponse::fromDomain)
                .orElseThrow(() -> new IllegalArgumentException("Promotion not found."));
    }
}
