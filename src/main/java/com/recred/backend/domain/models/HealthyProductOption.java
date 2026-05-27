package com.recred.backend.domain.models;

import java.util.UUID;

public record HealthyProductOption(
        UUID productId,
        String productName,
        String productDescription,
        String categoryName
) {
}
