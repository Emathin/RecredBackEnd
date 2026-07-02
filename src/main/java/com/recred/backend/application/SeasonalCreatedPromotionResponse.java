package com.recred.backend.application;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record SeasonalCreatedPromotionResponse(
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
}
