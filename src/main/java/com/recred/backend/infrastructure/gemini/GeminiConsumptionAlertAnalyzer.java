package com.recred.backend.infrastructure.gemini;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recred.backend.domain.models.ConsumptionAlertAnalysisRequest;
import com.recred.backend.domain.models.ConsumptionAlertMessages;
import com.recred.backend.domain.services.ConsumptionAlertAnalyzer;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class GeminiConsumptionAlertAnalyzer implements ConsumptionAlertAnalyzer {

    private static final Logger LOGGER = LoggerFactory.getLogger(
            GeminiConsumptionAlertAnalyzer.class);
    private static final int CONFIDENCE_SCALE = 4;
    private static final BigDecimal DEFAULT_CONFIDENCE = new BigDecimal("0.7000");
    private static final String PROMPT_TEMPLATE = """
            Sos un asistente de consumo escolar.
            Genera mensajes breves, claros y accionables para una app de recreo escolar.
            No recalcules estadisticas ni inventes importes.
            No uses tono alarmista, culpabilizador ni medico.
            El mensaje del student debe hablarle directamente al student.
            El mensaje del tutor debe invitarlo a acompaniar al student.
            Si el tipo es HEALTHY_RECOMMENDATION, sugeri una opcion saludable general.
            Si el tipo es STATISTICAL_PATTERN, basate en la category principal.
            Cada mensaje debe tener menos de 150 caracteres.
            Responde solo JSON valido con esta forma:
            {
              "tittle": "texto breve",
              "tituloTutor": "texto breve",
              "mensajeAlumno": "texto para student",
              "mensajeTutor": "texto para tutor",
              "accionSugerida": "texto breve",
              "confianza": 0.80
            }

            Datos:
            tipo=%s
            student=%s
            fechaCalculo=%s
            startDate=%s
            fechaFin=%s
            cantidadCompras=%s
            montoTotal=%s
            categoriaPrincipal=%s
            porcentajeCategoriaPrincipal=%s
            productoSugerido=%s
            """;

    private final GeminiTextClient geminiTextClient;
    private final ObjectMapper objectMapper;

    public GeminiConsumptionAlertAnalyzer(
            GeminiTextClient geminiTextClient,
            ObjectMapper objectMapper
    ) {
        this.geminiTextClient = geminiTextClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<ConsumptionAlertMessages> analyze(ConsumptionAlertAnalysisRequest request) {
        return geminiTextClient.generate(prompt(request))
                .flatMap(this::parseResponse);
    }

    private Optional<ConsumptionAlertMessages> parseResponse(GeminiTextResponse response) {
        try {
            GeminiAlertResponse payload = objectMapper.readValue(
                    response.text(),
                    GeminiAlertResponse.class
            );
            if (!payload.isValid()) {
                return Optional.empty();
            }
            return Optional.of(new ConsumptionAlertMessages(
                    payload.title(),
                    payload.tutorTitle(),
                    payload.studentMessage(),
                    payload.tutorMessage(),
                    payload.suggestedAction(),
                    confidence(payload.confidence()),
                    response.model()
            ));
        } catch (JsonProcessingException exception) {
            LOGGER.warn(
                    "Gemini returned invalid consumption alert analysis: {}",
                    exception.getOriginalMessage()
            );
            return Optional.empty();
        }
    }

    private String prompt(ConsumptionAlertAnalysisRequest request) {
        return PROMPT_TEMPLATE.formatted(
                request.type(),
                request.studentName(),
                request.calculationDate(),
                request.startDate(),
                request.endDate(),
                request.purchaseCount(),
                request.totalAmount(),
                textValue(request.mainCategory()),
                request.mainCategoryPercentage(),
                textValue(request.suggestedProductName())
        );
    }

    private BigDecimal confidence(BigDecimal confidence) {
        if (confidence == null) {
            return DEFAULT_CONFIDENCE;
        }
        return confidence.setScale(CONFIDENCE_SCALE, RoundingMode.HALF_UP);
    }

    private String textValue(String value) {
        if (value == null || value.isBlank()) {
            return "sin dato";
        }
        return value;
    }

    private record GeminiAlertResponse(
            @JsonProperty("tittle")
            String title,
            @JsonProperty("tituloTutor")
            String tutorTitle,
            @JsonProperty("mensajeAlumno")
            String studentMessage,
            @JsonProperty("mensajeTutor")
            String tutorMessage,
            @JsonProperty("accionSugerida")
            String suggestedAction,
            @JsonProperty("confianza")
            BigDecimal confidence
    ) {

        private boolean isValid() {
            return Stream.of(title, tutorTitle, studentMessage, tutorMessage, suggestedAction)
                    .allMatch(this::hasText);
        }

        private boolean hasText(String value) {
            return value != null && !value.isBlank();
        }
    }
}
