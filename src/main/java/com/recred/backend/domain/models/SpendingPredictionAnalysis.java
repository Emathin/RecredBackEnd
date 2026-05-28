package com.recred.backend.domain.models;

import java.util.List;

public record SpendingPredictionAnalysis(
        String summary,
        List<String> alerts,
        List<String> recommendations,
        String model
) {
}
