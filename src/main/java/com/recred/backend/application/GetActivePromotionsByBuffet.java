package com.recred.backend.application;

import com.recred.backend.domain.repositories.PromotionRepository;

import java.util.List;
import java.util.UUID;

public class GetActivePromotionsByBuffet {

    private final PromotionRepository promotionRepository;

    public GetActivePromotionsByBuffet(PromotionRepository promotionRepository) {
        this.promotionRepository = promotionRepository;
    }

    public List<PromotionResponse> execute(UUID buffetId) {
        return promotionRepository.findAllActiveByBuffet(buffetId).stream()
                .map(PromotionResponse::fromDomain)
                .collect(java.util.stream.Collectors.toList());
    }
}
