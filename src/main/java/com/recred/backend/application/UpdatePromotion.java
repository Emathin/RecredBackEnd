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

    public CreatePromotionResponse execute(UpdatePromotionRequest request) {
        Promotion promotion = findPromotion(request.id());
        updatePromotion(promotion, request);
        return CreatePromotionResponse.fromDomain(promotionRepository.save(promotion));
    }

    private void updatePromotion(Promotion promotion, UpdatePromotionRequest request) {
        promotion.updateDetails(
                productsFrom(request.productIds()),
                request.discountPercentage(),
                request.name(),
                request.startDate(),
                request.endDate(),
                LocalDateTime.now());
    }

    private Promotion findPromotion(UUID id) {
        return promotionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Promotion not found."));
    }

    private List<Producto> productsFrom(List<UUID> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return List.of();
        }

        return productIds.stream()
                .map(this::productWithId)
                .toList();
    }

    private Producto productWithId(UUID productId) {
        Producto product = new Producto();
        product.setId(productId);
        return product;
    }
}
