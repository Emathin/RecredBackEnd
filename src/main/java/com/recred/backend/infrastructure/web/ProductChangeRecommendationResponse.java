package com.recred.backend.infrastructure.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.recred.backend.domain.models.ProductChangeAnalysis;
import com.recred.backend.domain.models.ProductRecommendationResult;

import java.util.List;

public record ProductChangeRecommendationResponse(
        @JsonProperty("productoOriginal")
        String productoOriginal,
        @JsonProperty("resumen")
        String resumen,
        @JsonProperty("alertas")
        List<String> alertas,
        @JsonProperty("productosSugeridos")
        List<String> productosSugeridos,
        @JsonProperty("motivoIA")
        String motivoIA,
        @JsonProperty("modeloIA")
        String modeloIA
) {
    public static ProductChangeRecommendationResponse fromDomain(
            ProductRecommendationResult result) {
        ProductChangeAnalysis analysis = result.getAiAnalysis().orElse(null);
        
        return new ProductChangeRecommendationResponse(
                result.getProductChangeRecommendation().getNombre(),
                analysis != null ? analysis.summary() : null,
                analysis != null ? analysis.alerts() : List.of(),
                analysis != null ? analysis.recommendations() : List.of(),
                "Producto dentro del top 5 menos vendidos",
                analysis != null ? analysis.model() : null
        );
    }
}
