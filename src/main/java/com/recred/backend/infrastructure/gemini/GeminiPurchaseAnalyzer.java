package com.recred.backend.infrastructure.gemini;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.recred.backend.domain.models.PurchasePatternAnalysis;
import com.recred.backend.domain.models.PurchaseResume;
import com.recred.backend.domain.services.PurchaseAnalyzer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class GeminiPurchaseAnalyzer implements PurchaseAnalyzer {

    private static final Logger LOGGER = LoggerFactory.getLogger(
            GeminiSpendingPredictionAnalyzer.class);
    private static final String PROMPT_TEMPLATE = """
            Sos un asistente para alumnos de una app escolar.
            Se te brindaran una lista de compras
            Deberas detectar si hay un patron de compra entre un producto y un dia de la semana
            en "patronDiasDetectado incluye un solo dia,
             en mayusculas,en ingles, singular y sin acentos
            Usa lenguaje simple, claro y accionable para usuario final.
            No menciones modelo, confianza, calidad predictiva ni cantidad
             de datos historicos.
            El resumen debe tener una sola oracion.
            Devolve como maximo 1 recomendaciones.
            Cada texto debe tener menos de 140 caracteres.
            Responde solo JSON valido con esta forma:
            {
              "resumen": "texto breve",
              "product_id": "id de producto proporcionado",
              "productName": "nombre del producto proporcionado",
              "patronDiasDetectado": "day"
            }

            Datos:
            purchases=%s
           
            """;

    private final GeminiTextClient geminiTextClient;
    private final ObjectMapper objectMapper;

    public GeminiPurchaseAnalyzer(GeminiTextClient geminiTextClient
            , ObjectMapper objectMapper) {
        this.geminiTextClient = geminiTextClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<PurchasePatternAnalysis> analyze(List<PurchaseResume> purchases) {

        String resumeToString = purchases.stream().map(PurchaseResume::toString)
                .collect(Collectors.joining(", "));
        String promptToGemini = prompt(resumeToString);

        Optional<GeminiTextResponse> response =
                geminiTextClient.generate(promptToGemini);

        return this.parseResponse(response.get());
    }
    private String prompt(
            String purchases) {
        return PROMPT_TEMPLATE.formatted(

                purchases
        );
    }

    private Optional<PurchasePatternAnalysis> parseResponse(GeminiTextResponse response) {
        try {
            GeminiPurchaseAnalyzer.GeminiAnalysisResponse payload = objectMapper.readValue(
                    response.text(),
                    GeminiPurchaseAnalyzer.GeminiAnalysisResponse.class
            );
            return Optional.of(new PurchasePatternAnalysis(
                    payload.summary(),
                    payload.product_id(),
                    payload.productName(),
                    payload.purchaseDays()

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
            @JsonProperty("product_id")
            UUID product_id,

            @JsonProperty("productName")
            String productName,
            @JsonProperty("patronDiasDetectado")
            String purchaseDays
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
