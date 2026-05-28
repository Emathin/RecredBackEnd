package com.recred.backend.infrastructure.gemini;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recred.backend.domain.models.HealthyRecommendationAnalysisRequest;
import com.recred.backend.domain.models.HealthyRecommendationMessages;
import com.recred.backend.domain.services.HealthyRecommendationAnalyzer;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class GeminiHealthyRecommendationAnalyzer implements HealthyRecommendationAnalyzer {

    private static final Logger LOGGER = LoggerFactory.getLogger(
            GeminiHealthyRecommendationAnalyzer.class);
    private static final int CONFIDENCE_SCALE = 4;
    private static final BigDecimal DEFAULT_CONFIDENCE = new BigDecimal("0.7500");
    private static final String PROMPT_TEMPLATE = """
            Sos un asistente de consumo escolar.
            Analiza el historial reciente y las opciones disponibles del buffet.
            Si corresponde, elegi una opcion mas saludable para recomendarle al alumno.
            Si no hay una recomendacion clara, responde recomendar=false.
            No inventes productos ni uses un producto fuera de opcionesDisponibles.
            No uses tono alarmista, culpabilizador ni medico.
            El mensaje debe hablarle directamente al alumno.
            El mensaje debe tener menos de 150 caracteres.
            Responde solo JSON valido con esta forma:
            {
              "recomendar": true,
              "productoIdSugerido": "uuid de opcionesDisponibles",
              "titulo": "texto breve",
              "mensajeAlumno": "texto para alumno nombrando el producto elegido",
              "accionSugerida": "texto breve",
              "confianza": 0.80
            }
            Si no corresponde recomendar:
            {"recomendar": false}

            Contexto:
            %s
            """;

    private final GeminiTextClient geminiTextClient;
    private final ObjectMapper objectMapper;

    public GeminiHealthyRecommendationAnalyzer(
            GeminiTextClient geminiTextClient,
            ObjectMapper objectMapper
    ) {
        this.geminiTextClient = geminiTextClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<HealthyRecommendationMessages> analyze(
            HealthyRecommendationAnalysisRequest request
    ) {
        return geminiTextClient.generate(prompt(request))
                .flatMap(this::parseResponse);
    }

    private Optional<HealthyRecommendationMessages> parseResponse(GeminiTextResponse response) {
        try {
            GeminiHealthyRecommendationResponse payload = objectMapper.readValue(
                    response.text(),
                    GeminiHealthyRecommendationResponse.class
            );
            if (!payload.isValid()) {
                return Optional.empty();
            }

            return Optional.of(new HealthyRecommendationMessages(
                    payload.title(),
                    payload.studentMessage(),
                    payload.suggestedAction(),
                    payload.suggestedProductId(),
                    confidence(payload.confidence()),
                    response.model()
            ));
        } catch (JsonProcessingException exception) {
            LOGGER.warn(
                    "Gemini returned invalid healthy recommendation analysis: {}",
                    exception.getOriginalMessage()
            );
            return Optional.empty();
        }
    }

    private String prompt(HealthyRecommendationAnalysisRequest request) {
        return PROMPT_TEMPLATE.formatted(objectMapper.valueToTree(payload(request)).toString());
    }

    private BigDecimal confidence(BigDecimal confidence) {
        if (confidence == null) {
            return DEFAULT_CONFIDENCE;
        }

        return confidence.setScale(CONFIDENCE_SCALE, RoundingMode.HALF_UP);
    }

    private record GeminiHealthyRecommendationResponse(
            @JsonProperty("recomendar")
            Boolean recommend,
            @JsonProperty("productoIdSugerido")
            UUID suggestedProductId,
            @JsonProperty("titulo")
            String title,
            @JsonProperty("mensajeAlumno")
            String studentMessage,
            @JsonProperty("accionSugerida")
            String suggestedAction,
            @JsonProperty("confianza")
            BigDecimal confidence
    ) {

        private boolean isValid() {
            if (!Boolean.TRUE.equals(recommend)) {
                return false;
            }
            return suggestedProductId != null
                    && Stream.of(title, studentMessage, suggestedAction).allMatch(this::hasText);
        }

        private boolean hasText(String value) {
            return value != null && !value.isBlank();
        }
    }

    private PromptPayload payload(HealthyRecommendationAnalysisRequest request) {
        return new PromptPayload(
                request.studentName(),
                request.calculationDate().toString(),
                request.startDate().toString(),
                request.endDate().toString(),
                request.repeatedProductName(),
                request.repeatedProductCategory(),
                request.repeatedDays(),
                request.totalQuantity(),
                request.consumptionHistory().stream()
                        .map(consumption -> new PromptConsumption(
                                consumption.productId(),
                                consumption.productName(),
                                consumption.productDescription(),
                                consumption.categoryName(),
                                consumption.purchaseDateTime().toString(),
                                consumption.quantity()
                        ))
                        .toList(),
                request.availableOptions().stream()
                        .map(option -> new PromptOption(
                                option.productId(),
                                option.productName(),
                                option.productDescription(),
                                option.categoryName()
                        ))
                        .toList()
        );
    }

    private record PromptPayload(
            String alumno,
            String fechaCalculo,
            String fechaInicio,
            String fechaFin,
            String productoRepetido,
            String categoriaProductoRepetido,
            int diasRepetidos,
            long cantidadTotal,
            List<PromptConsumption> historialConsumo,
            List<PromptOption> opcionesDisponibles
    ) {
    }

    private record PromptConsumption(
            UUID productoId,
            String nombre,
            String descripcion,
            String categoria,
            String fechaHora,
            Integer cantidad
    ) {
    }

    private record PromptOption(
            UUID productoId,
            String nombre,
            String descripcion,
            String categoria
    ) {
    }
}
