package com.recred.backend.domain.models;

import java.time.LocalDateTime;
import java.util.UUID;

public record RecentProductConsumption(
        UUID productId,
        String productName,
        String productDescription,
        String categoryName,
        UUID buffetId,
        LocalDateTime purchaseDateTime,
        Integer quantity
) {
}
