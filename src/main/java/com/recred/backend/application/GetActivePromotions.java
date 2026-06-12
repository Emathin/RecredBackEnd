package com.recred.backend.application;

import com.recred.backend.domain.models.Promotion;
import com.recred.backend.domain.repositories.PromotionRepository;

import java.util.List;

public class GetActivePromotions {

    private final PromotionRepository promotionRepository;

    public GetActivePromotions(PromotionRepository promotionRepository) {
        this.promotionRepository = promotionRepository;
    }

    public List<Promotion> execute() {
        return promotionRepository.findAllActive();
    }
}
