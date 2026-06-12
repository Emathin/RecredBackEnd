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

    public Promotion execute(
            UUID id, 
            List<UUID> productIds, 
            BigDecimal discountPercentage, 
            String name, 
            LocalDateTime startDate, 
            LocalDateTime endDate) {
            
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Promotion not found."));
                
        if (productIds != null && !productIds.isEmpty()) {
            if (!hasMinimumProducts(productIds)) {
                throw new IllegalArgumentException("A promotion requires at least 2 products.");
            }
            List<Producto> products = productIds.stream().map(productId -> {
                Producto p = new Producto();
                p.setId(productId);
                return p;
            }).collect(Collectors.toList());
            promotion.setProducts(products);
        }
        
        if (discountPercentage != null) {
            if (!isDiscountValid(discountPercentage)) {
                throw new IllegalArgumentException(
                        "Discount percentage must be between 0 and 100.");
            }
            promotion.setDiscountPercentage(discountPercentage);
        }
        
        if (name != null) {
            promotion.setName(name);
        }
        
        if (startDate != null) {
            promotion.setStartDate(startDate);
        }
        
        if (endDate != null) {
            promotion.setEndDate(endDate);
        }
        
        if (!isDateRangeValid(promotion.getStartDate(), promotion.getEndDate())) {
            throw new IllegalArgumentException("Invalid date range.");
        }
        
        if (isPromotionCurrentlyActive(promotion.getStartDate(), promotion.getEndDate())) {
            promotion.setStatus(PromotionStatus.ACTIVE);
        } else {
            promotion.setStatus(PromotionStatus.DRAFT);
        }

        return promotionRepository.save(promotion);
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
