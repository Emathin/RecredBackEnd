package com.recred.backend.domain.models;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

public record PriceAlertSuggestion(
        UUID studentId,
        UUID productId,
        UUID buffetId,
        Set<UUID> tutorUserIds,
        String productName,
        String title,
        String message,
        BigDecimal oldPrice,
        BigDecimal newPrice,
        String aiReason
) {
}
