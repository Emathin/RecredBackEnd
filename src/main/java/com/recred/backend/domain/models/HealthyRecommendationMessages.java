package com.recred.backend.domain.models;

import java.math.BigDecimal;
import java.util.UUID;

public record HealthyRecommendationMessages(
        String title,
        String studentMessage,
        String suggestedAction,
        UUID suggestedProductId,
        BigDecimal confidence,
        String model
) {
}
