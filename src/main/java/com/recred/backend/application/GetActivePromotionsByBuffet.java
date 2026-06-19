package com.recred.backend.application;

import com.recred.backend.domain.models.Promotion;
import com.recred.backend.domain.repositories.PromotionRepository;

import java.util.List;
import java.util.UUID;

public class GetActivePromotionsByBuffet {

    private final PromotionRepository promotionRepository;

    public GetActivePromotionsByBuffet(PromotionRepository promotionRepository) {
        this.promotionRepository = promotionRepository;
    }

    public List<Promotion> execute(UUID buffetId) {
        return promotionRepository.findAllActiveByBuffet(buffetId);
    }
}
