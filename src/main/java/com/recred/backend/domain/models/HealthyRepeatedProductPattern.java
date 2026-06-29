package com.recred.backend.domain.models;

import java.time.LocalDateTime;
import java.util.UUID;

public record HealthyRepeatedProductPattern(
        UUID productId,
        String productName,
        String categoryName,
        UUID buffetId,
        int repeatedDays,
        long totalQuantity,
        LocalDateTime latestPurchaseDateTime
) {

    public boolean hasAtLeastRepeatedDays(int minimumRepeatedDays) {
        return repeatedDays >= minimumRepeatedDays;
    }
}
