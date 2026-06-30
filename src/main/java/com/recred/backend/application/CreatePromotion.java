package com.recred.backend.application;

import com.recred.backend.domain.models.Buffet;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Promotion;
import com.recred.backend.domain.models.PromotionStatus;
import com.recred.backend.domain.repositories.PromotionRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class CreatePromotion {

    private final PromotionRepository promotionRepository;

    public CreatePromotion(PromotionRepository promotionRepository) {
        this.promotionRepository = promotionRepository;
    }

    public CreatePromotionResponse execute(CreatePromotionRequest request) {

        if (!hasMinimumProducts(request.productIds())) {
            throw new IllegalArgumentException("A promotion requires at least 2 products.");
        }
        if (!isDiscountValid(request.discountPercentage())) {
            throw new IllegalArgumentException("Discount percentage must be between 0 and 100.");
        }
        if (!isDateRangeValid(request.startDate(), request.endDate())) {
            throw new IllegalArgumentException("Invalid date range.");
        }

        Promotion promotion = new Promotion();
        promotion.setName(request.name());
        promotion.setDiscountPercentage(request.discountPercentage());
        promotion.setStartDate(request.startDate());
        promotion.setEndDate(request.endDate());
        promotion.setImageUrl(request.imageUrl());

        if (request.buffetId() != null) {
            Buffet buffet = new Buffet();
            buffet.setId(request.buffetId());
            promotion.setBuffet(buffet);
        }

        if (request.forceDraft()) {
            promotion.setStatus(PromotionStatus.DRAFT);
        } else if (isPromotionCurrentlyActive(request.startDate(), request.endDate())) {
            promotion.setStatus(PromotionStatus.ACTIVE);
        } else {
            promotion.setStatus(PromotionStatus.DRAFT);
        }

        List<Producto> products = request.productIds().stream().map(id -> {
            Producto p = new Producto();
            p.setId(id);
            return p;
        }).collect(Collectors.toList());

        promotion.setProducts(products);

        Promotion savedPromotion = promotionRepository.save(promotion);
        return CreatePromotionResponse.fromDomain(savedPromotion);
    }


    private boolean hasMinimumProducts(List<UUID> productIds) {
        return productIds != null && productIds.size() >= 2;
    }

    private boolean isDiscountValid(BigDecimal discountPercentage) {
        return discountPercentage != null 
                && discountPercentage.compareTo(BigDecimal.ZERO) > 0 
                && discountPercentage.compareTo(new BigDecimal("100")) <= 0;
    }

    private boolean isDateRangeValid(LocalDateTime startDate, LocalDateTime endDate) {
        return startDate != null && endDate != null && startDate.isBefore(endDate);
    }
    
    private boolean isPromotionCurrentlyActive(LocalDateTime startDate, LocalDateTime endDate) {
        LocalDateTime now = LocalDateTime.now();
        return !now.isBefore(startDate) && !now.isAfter(endDate);
    }
}
