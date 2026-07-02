package com.recred.backend.application;

import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Promotion;
import com.recred.backend.domain.models.PromotionStatus;
import com.recred.backend.domain.repositories.PromotionRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class UpdatePromotion {

    private final PromotionRepository promotionRepository;

    public UpdatePromotion(PromotionRepository promotionRepository) {
        this.promotionRepository = promotionRepository;
    }

    public CreatePromotionResponse execute(UpdatePromotionRequest request) {
        Promotion promotion = promotionRepository.findById(request.id())
                .orElseThrow(() -> new IllegalArgumentException("Promotion not found."));
                
        if (request.productIds() != null && !request.productIds().isEmpty()) {
            if (!hasMinimumProducts(request.productIds())) {
                throw new IllegalArgumentException("A promotion requires at least 2 products.");
            }
            List<Producto> products = request.productIds().stream().map(productId -> {
                Producto p = new Producto();
                p.setId(productId);
                return p;
            }).collect(Collectors.toList());
            promotion.setProducts(products);
        }
        
        if (request.discountPercentage() != null) {
            if (!isDiscountValid(request.discountPercentage())) {
                throw new IllegalArgumentException(
                        "Discount percentage must be between 0 and 100.");
            }
            promotion.setDiscountPercentage(request.discountPercentage());
        }
        
        if (request.name() != null) {
            promotion.setName(request.name());
        }
        
        if (request.startDate() != null) {
            promotion.setStartDate(request.startDate());
        }
        
        if (request.endDate() != null) {
            promotion.setEndDate(request.endDate());
        }
        
        if (!isDateRangeValid(promotion.getStartDate(), promotion.getEndDate())) {
            throw new IllegalArgumentException("Invalid date range.");
        }
        
        if (isPromotionCurrentlyActive(promotion.getStartDate(), promotion.getEndDate())) {
            promotion.setStatus(PromotionStatus.ACTIVE);
        } else {
            promotion.setStatus(PromotionStatus.DRAFT);
        }

        return CreatePromotionResponse.fromDomain(promotionRepository.save(promotion));
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
