package com.recred.backend.application;

import com.recred.backend.domain.models.Buffet;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Promotion;
import com.recred.backend.domain.models.PromotionStatus;
import com.recred.backend.domain.repositories.PromotionRepository;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class CreatePromotion {

    private static final BigDecimal MAXIMUM_DISCOUNT_PERCENTAGE = new BigDecimal("100");
    private static final int MINIMUM_PRODUCT_COUNT = 2;

    private final PromotionRepository promotionRepository;
    private final Clock clock;

    public CreatePromotion(PromotionRepository promotionRepository) {
        this(promotionRepository, Clock.systemDefaultZone());
    }

    public CreatePromotion(PromotionRepository promotionRepository, Clock clock) {
        this.promotionRepository = promotionRepository;
        this.clock = clock;
    }

    public CreatePromotionResponse execute(CreatePromotionRequest promotionRequest) {
        ensurePromotionCreationRulesAreMet(promotionRequest);
        Promotion promotion = buildPromotionFrom(promotionRequest);
        promotion.setStatus(determineInitialStatus(promotionRequest));

        Promotion savedPromotion = promotionRepository.save(promotion);
        return CreatePromotionResponse.fromDomain(savedPromotion);
    }

    private void ensurePromotionCreationRulesAreMet(CreatePromotionRequest promotionRequest) {
        if (!hasMinimumProducts(promotionRequest.productIds())) {
            throw new IllegalArgumentException("A promotion requires at least 2 products.");
        }
        if (!isDiscountValid(promotionRequest.discountPercentage())) {
            throw new IllegalArgumentException("Discount percentage must be between 0 and 100.");
        }
        if (!isDateRangeValid(promotionRequest.startDate(), promotionRequest.endDate())) {
            throw new IllegalArgumentException("Invalid date range.");
        }
    }

    private Promotion buildPromotionFrom(CreatePromotionRequest promotionRequest) {
        Promotion promotion = new Promotion();
        promotion.setName(promotionRequest.name());
        promotion.setDiscountPercentage(promotionRequest.discountPercentage());
        promotion.setStartDate(promotionRequest.startDate());
        promotion.setEndDate(promotionRequest.endDate());
        promotion.setImageUrl(promotionRequest.imageUrl());
        promotion.setBuffet(buffetFrom(promotionRequest.buffetId()));
        promotion.setProducts(productsFrom(promotionRequest.productIds()));
        return promotion;
    }

    private Buffet buffetFrom(UUID buffetId) {
        if (buffetId == null) {
            return null;
        }

        Buffet buffet = new Buffet();
        buffet.setId(buffetId);
        return buffet;
    }

    private List<Producto> productsFrom(List<UUID> productIds) {
        return productIds.stream()
                .map(this::productFrom)
                .collect(Collectors.toList());
    }

    private Producto productFrom(UUID productId) {
        Producto product = new Producto();
        product.setId(productId);
        return product;
    }

    private PromotionStatus determineInitialStatus(CreatePromotionRequest promotionRequest) {
        if (promotionRequest.forceDraft()) {
            return PromotionStatus.DRAFT;
        }
        if (isPromotionCurrentlyActive(promotionRequest.startDate(), promotionRequest.endDate())) {
            return PromotionStatus.ACTIVE;
        }
        return PromotionStatus.DRAFT;
    }

    private boolean hasMinimumProducts(List<UUID> productIds) {
        return productIds.size() >= MINIMUM_PRODUCT_COUNT;
    }

    private boolean isDiscountValid(BigDecimal discountPercentage) {
        return discountPercentage.compareTo(BigDecimal.ZERO) > 0
                && discountPercentage.compareTo(MAXIMUM_DISCOUNT_PERCENTAGE) <= 0;
    }

    private boolean isDateRangeValid(LocalDateTime startDate, LocalDateTime endDate) {
        return startDate.isBefore(endDate);
    }
    
    private boolean isPromotionCurrentlyActive(LocalDateTime startDate, LocalDateTime endDate) {
        LocalDateTime now = LocalDateTime.now(clock);
        return !now.isBefore(startDate) && !now.isAfter(endDate);
    }
}
