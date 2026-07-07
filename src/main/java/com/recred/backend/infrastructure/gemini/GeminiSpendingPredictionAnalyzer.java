package com.recred.backend.infrastructure.gemini;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recred.backend.domain.models.ConsumptionCategory;
import com.recred.backend.domain.models.SpendingPrediction;
import com.recred.backend.domain.models.SpendingPredictionAnalysis;
import com.recred.backend.domain.services.SpendingPredictionAnalyzer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class GeminiSpendingPredictionAnalyzer implements SpendingPredictionAnalyzer {

    private static final Logger LOGGER = LoggerFactory.getLogger(
            GeminiSpendingPredictionAnalyzer.class);
    private static final String CATEGORY_SEPARATOR = "=";
    private static final String PROMPT_TEMPLATE = """
            Sos un asistente para tutores y alumnos de una app escolar.
            Analiza una prediccion de gasto sin recalcular los importes.
            Usa lenguaje simple, claro y accionable.
            El mensaje sera leido por los tutores (padres), por lo tanto DEBE redactarse
            refiriendose al alumno/hijo en tercera persona. Nunca le hables directamente al
            usuario como si el fuera quien realiza los consumos (Ejemplo: usa "El gasto de
            tu hijo", "El alumno consumio").
            No menciones modelo, confianza, calidad predictiva ni cantidad de datos historicos.
            No digas que el gasto proyectado sube respecto al actual como alerta: eso es esperable.
            Genera alertas solo si hay riesgo real o un punto concreto para revisar.
            Si el gasto proyectado esta lejos del limite, indica que no hay riesgo de superarlo.
            El resumen debe tener una sola oracion.
            Devolve como maximo 2 alertas y 2 recomendaciones.
            Cada texto debe tener menos de 140 caracteres.
            Responde solo JSON valido con esta forma:
            {
              "resumen": "texto breve sobre el hijo/alumno",
              "alertas": ["texto sobre el hijo/alumno"],
              "recomendaciones": ["texto para el padre sobre el hijo/alumno"]
            }

            Datos:
            periodo=%s
            fechaCalculo=%s
            fechaInicio=%s
            fechaFin=%s
            gastoActual=%s
            gastoPredicho=%s
            promedioGastoDiario=%s
            montoLimite=%s
            porcentajePresupuesto=%s
            diasRestantes=%s
            categoriasMasConsumidas=%s
            """;

    private final GeminiTextClient geminiTextClient;
    private final ObjectMapper objectMapper;

    public GeminiSpendingPredictionAnalyzer(
            GeminiTextClient geminiTextClient,
            ObjectMapper objectMapper
    ) {
        this.geminiTextClient = geminiTextClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<SpendingPredictionAnalysis> analyze(SpendingPrediction spendingPrediction) {
        return geminiTextClient.generate(prompt(spendingPrediction))
                .flatMap(this::parseResponse);
    }

    private Optional<SpendingPredictionAnalysis> parseResponse(GeminiTextResponse response) {
        try {
            GeminiAnalysisResponse payload = objectMapper.readValue(
                    response.text(),
                    GeminiAnalysisResponse.class
            );
            return Optional.of(new SpendingPredictionAnalysis(
                    payload.summary(),
                    normalizeList(payload.alerts()),
                    normalizeList(payload.recommendations()),
                    response.model()
            ));
        } catch (JsonProcessingException exception) {
            LOGGER.warn(
                    "Gemini returned invalid spending prediction analysis: {}",
                    exception.getMessage()
            );
            return Optional.empty();
        }
    }

    private String prompt(SpendingPrediction spendingPrediction) {
        return PROMPT_TEMPLATE.formatted(
                spendingPrediction.period(),
                spendingPrediction.calculationDate(),
                spendingPrediction.startDate(),
                spendingPrediction.endDate(),
                spendingPrediction.currentSpending(),
                spendingPrediction.predictedSpending(),
                spendingPrediction.averageDailySpending(),
                spendingPrediction.limitAmount(),
                spendingPrediction.budgetPercentage(),
                spendingPrediction.remainingDays(),
                categories(spendingPrediction.mostConsumedCategories())
        );
    }

    private String categories(List<ConsumptionCategory> categories) {
        return categories.stream()
                .map(category -> category.description()
                        + CATEGORY_SEPARATOR
                        + category.totalAmount())
                .toList()
                .toString();
    }

    private List<String> normalizeList(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .toList();
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
}
