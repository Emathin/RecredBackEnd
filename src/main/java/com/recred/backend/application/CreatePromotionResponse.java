package com.recred.backend.application;

import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Promotion;
import com.recred.backend.domain.models.PromotionStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public record CreatePromotionResponse(
        UUID id,
        String name,
        BigDecimal discountPercentage,
        LocalDateTime startDate,
        LocalDateTime endDate,
        PromotionStatus status,
        String imageUrl,
        UUID buffetId,
        List<UUID> productIds
) {
    public static CreatePromotionResponse fromDomain(Promotion promotion) {
        return new CreatePromotionResponse(
                promotion.getId(),
                promotion.getName(),
                promotion.getDiscountPercentage(),
                promotion.getStartDate(),
                promotion.getEndDate(),
                promotion.getStatus(),
                promotion.getImageUrl(),
                promotion.getBuffet() != null ? promotion.getBuffet().getId() : null,
                promotion.getProducts() != null ? promotion.getProducts().stream()
                        .map(Producto::getId)
                        .collect(Collectors.toList()) : List.of()
        );
    }
}
