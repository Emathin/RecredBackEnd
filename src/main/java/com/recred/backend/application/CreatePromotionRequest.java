package com.recred.backend.application;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CreatePromotionRequest(
        List<UUID> productIds,
        BigDecimal discountPercentage,
        String name,
        LocalDateTime startDate,
        LocalDateTime endDate,
        UUID buffetId,
        boolean forceDraft,
        String imageUrl
) {
}
