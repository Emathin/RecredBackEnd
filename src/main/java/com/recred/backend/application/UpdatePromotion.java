package com.recred.backend.application;

import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Promotion;
import com.recred.backend.domain.repositories.PromotionRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class UpdatePromotion {

    private final PromotionRepository promotionRepository;

    public UpdatePromotion(PromotionRepository promotionRepository) {
        this.promotionRepository = promotionRepository;
    }

    public CreatePromotionResponse execute(UpdatePromotionRequest updateRequest) {
        Promotion promotion = findPromotionToUpdate(updateRequest.id());
        applyPromotionUpdate(promotion, updateRequest);
        return CreatePromotionResponse.fromDomain(promotionRepository.save(promotion));
    }

    private void applyPromotionUpdate(Promotion promotion, UpdatePromotionRequest updateRequest) {
        promotion.updateDetails(
                productReferencesFrom(updateRequest.productIds()),
                updateRequest.discountPercentage(),
                updateRequest.name(),
                updateRequest.startDate(),
                updateRequest.endDate(),
                LocalDateTime.now());
    }

    private Promotion findPromotionToUpdate(UUID promotionId) {
        return promotionRepository.findById(promotionId)
                .orElseThrow(() -> new IllegalArgumentException("Promotion not found."));
    }

    private List<Producto> productReferencesFrom(List<UUID> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return List.of();
        }

        return productIds.stream()
                .map(this::productReference)
                .toList();
    }

    private Producto productReference(UUID productId) {
        Producto product = new Producto();
        product.setId(productId);
        return product;
    }
}
