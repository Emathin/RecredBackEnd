package com.recred.backend.infrastructure.gemini;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recred.backend.domain.models.ProductChangeAnalysis;
import com.recred.backend.domain.models.ProductChangeRecommendation;
import com.recred.backend.domain.models.SpendingPrediction;
import com.recred.backend.domain.models.SpendingPredictionAnalysis;
import com.recred.backend.domain.services.ProductChangeAnalyzer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class GeminiproductChangeAnalyzer implements ProductChangeAnalyzer {

    private static final Logger LOGGER = LoggerFactory.getLogger(
            GeminiSpendingPredictionAnalyzer.class);
    private static final String CATEGORY_SEPARATOR = "=";
    private static final String PROMPT_TEMPLATE = """
            Sos un asistente para quioskeros de una app escolar.
            Se te brindara un producto y su precio.
            Devolveras una sugerencia para cambiar el producto por uno del mismo rubro existente en argentina
            Usa lenguaje simple, claro y accionable para usuario final.
            No menciones modelo, confianza, calidad predictiva ni cantidad de datos historicos.
            
            Genera alertas solo si hay riesgo real o un punto concreto para revisar.
            El resumen debe tener una sola oracion.
            Devolve como maximo 2 alertas y 2 recomendaciones.
            Cada texto debe tener menos de 140 caracteres.
            Responde solo JSON valido con esta forma:
            {
              "resumen": "texto breve",
              "alertas": ["texto"],
              "recomendaciones": ["texto"]
            }

            Datos:
            nombreProducto=%s
            precioProducto=%s
            """;

    private final GeminiTextClient geminiTextClient;
    private final ObjectMapper objectMapper;

    public GeminiproductChangeAnalyzer(
            GeminiTextClient geminiTextClient,
            ObjectMapper objectMapper
    ) {
        this.geminiTextClient = geminiTextClient;
        this.objectMapper = objectMapper;
    }
    @Override
    public Optional<ProductChangeAnalysis> analyze(ProductChangeRecommendation productChangeRecommendation) {
        return geminiTextClient.generate(prompt(productChangeRecommendation))
                .flatMap(this::parseResponse);
    }

    private String prompt(ProductChangeRecommendation productChangeRecommendation) {
        return PROMPT_TEMPLATE.formatted(

                productChangeRecommendation.getNombre(),
                productChangeRecommendation.getPrecio()
        );
    }

    private Optional<ProductChangeAnalysis> parseResponse(GeminiTextResponse response) {
        try {
            GeminiproductChangeAnalyzer.GeminiAnalysisResponse payload = objectMapper.readValue(
                    response.text(),
                    GeminiproductChangeAnalyzer.GeminiAnalysisResponse.class
            );
            return Optional.of(new ProductChangeAnalysis(
                    payload.summary(),
                    normalizeList(payload.alerts()),
                    normalizeList(payload.recommendations()),
                    response.model()
            ));
        } catch (JsonProcessingException exception) {
            LOGGER.warn(
                    "Gemini returned invalid spending prediction analysis: {}",
                    exception.getOriginalMessage()
            );
            return Optional.empty();
        }
    }

    private record GeminiAnalysisResponse(
            @JsonProperty("resumen")
            String summary,
            @JsonProperty("alertas")
            List<String> alerts,
            @JsonProperty("recomendaciones")
            List<String> recommendations
    ) {
    }
    private List<String> normalizeList(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .toList();
    }
}
