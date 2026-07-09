package com.recred.backend.application;

import com.recred.backend.domain.models.Promotion;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

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
        return new PromotionResponse(
                promotion.getId(),
                promotion.getName(),
                promotion.getDiscountPercentage(),
                promotion.getStartDate(),
                promotion.getEndDate(),
                statusName(promotion),
                productIds(promotion),
                buffetId(promotion),
                promotion.getImageUrl()
        );
    }

    private static String statusName(Promotion promotion) {
        return promotion.getStatus() == null ? null : promotion.getStatus().name();
    }

    private static List<UUID> productIds(Promotion promotion) {
        if (promotion.getProducts() == null) {
            return null;
        }

        return promotion.getProducts().stream()
                .map(product -> product.getId())
                .toList();
    }

    private static UUID buffetId(Promotion promotion) {
        return promotion.getBuffet() == null ? null : promotion.getBuffet().getId();
    }
}
