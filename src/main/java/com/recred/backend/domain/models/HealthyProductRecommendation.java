package com.recred.backend.domain.models;

public record HealthyProductRecommendation(
        ConsumptionAlertCandidate candidate,
        HealthyRecommendationPattern pattern,
        HealthyRecommendationMessages messages
) {
}
