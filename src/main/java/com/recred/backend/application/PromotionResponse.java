package com.recred.backend.application;

import com.recred.backend.domain.models.Promotion;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public record PromotionResponse(
        UUID id,
        String name,
        BigDecimal discountPercentage,
        LocalDateTime startDate,
        LocalDateTime endDate,
        String status,
        List<UUID> productIds,
        UUID buffetId,
        String imageUrl
) {
    public static PromotionResponse fromDomain(Promotion promotion) {
        List<UUID> productIds = null;
        if (promotion.getProducts() != null) {
            productIds = promotion.getProducts().stream()
                    .map(p -> p.getId())
                    .collect(Collectors.toList());
        }

        UUID buffetId = null;
        if (promotion.getBuffet() != null) {
            buffetId = promotion.getBuffet().getId();
        }

        return new PromotionResponse(
                promotion.getId(),
                promotion.getName(),
                promotion.getDiscountPercentage(),
                promotion.getStartDate(),
                promotion.getEndDate(),
                promotion.getStatus() != null ? promotion.getStatus().name() : null,
                productIds,
                buffetId,
                promotion.getImageUrl()
        );
    }
}
