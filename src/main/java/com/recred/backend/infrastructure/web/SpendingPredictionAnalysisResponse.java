package com.recred.backend.infrastructure.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.recred.backend.domain.models.SpendingPredictionAnalysis;
import java.util.List;

public record SpendingPredictionAnalysisResponse(
        @JsonProperty("resumen")
        String summary,
        @JsonProperty("alertas")
        List<String> alerts,
        @JsonProperty("recomendaciones")
        List<String> recommendations,
        @JsonProperty("modelo")
        String model
) {

    public static SpendingPredictionAnalysisResponse fromDomain(
            SpendingPredictionAnalysis analysis
    ) {
        return new SpendingPredictionAnalysisResponse(
                analysis.summary(),
                analysis.alerts(),
                analysis.recommendations(),
                analysis.model()
        );
    }
}
