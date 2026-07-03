package com.recred.backend.application;

import java.util.UUID;

public record GenerateSeasonalRecommendationRequest(
        Double latitude,
        Double longitude,
        UUID buffetId
) {
}
