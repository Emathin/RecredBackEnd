package com.recred.backend.infrastructure.gemini;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recred.backend.domain.models.BudgetPeriod;
import com.recred.backend.domain.models.SpendingPredictionAnalysis;
import com.recred.backend.domain.models.ConsumptionCategory;
import com.recred.backend.domain.models.SpendingPrediction;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GeminiSpendingPredictionAnalyzerTest {

    private final GeminiTextClient geminiTextClient = mock(GeminiTextClient.class);
    private final GeminiSpendingPredictionAnalyzer analyzer =
            new GeminiSpendingPredictionAnalyzer(geminiTextClient, new ObjectMapper());

    @Test
    void parseGeminiJsonResponse() {
        when(geminiTextClient.generate(contains("gastoPredicho=120.00"))).thenReturn(
                Optional.of(new GeminiTextResponse("""
                        {
                          "resumen": "El gasto proyectado es alto.",
                          "alertas": ["Puede superar el presupuesto."],
                          "recomendaciones": ["Revisar compras de bebidas."]
                        }
                        """, "gemini-test"))
        );

        Optional<SpendingPredictionAnalysis> analysis = analyzer.analyze(prediction());

        assertTrue(analysis.isPresent());
        assertEquals("El gasto proyectado es alto.", analysis.orElseThrow().summary());
        assertEquals("Puede superar el presupuesto.", analysis.orElseThrow().alerts().get(0));
        assertEquals("Revisar compras de bebidas.", analysis.orElseThrow().recommendations()
                .get(0));
        assertEquals("gemini-test", analysis.orElseThrow().model());
    }

    @Test
    void returnEmptyWhenGeminiDoesNotGenerateText() {
        when(geminiTextClient.generate(contains("gastoPredicho=120.00"))).thenReturn(
                Optional.empty()
        );

        Optional<SpendingPredictionAnalysis> analysis = analyzer.analyze(prediction());

        assertTrue(analysis.isEmpty());
    }

    @Test
    void returnEmptyWhenGeminiReturnsInvalidJson() {
        when(geminiTextClient.generate(contains("gastoPredicho=120.00"))).thenReturn(
                Optional.of(new GeminiTextResponse("no es json", "gemini-test"))
        );

        Optional<SpendingPredictionAnalysis> analysis = analyzer.analyze(prediction());

        assertTrue(analysis.isEmpty());
    }

    @Test
    void normalizeNullListsAndBlankItems() {
        when(geminiTextClient.generate(contains("gastoPredicho=120.00"))).thenReturn(
                Optional.of(new GeminiTextResponse("""
                        {
                          "resumen": "Resumen",
                          "alertas": null,
                          "recomendaciones": ["", "Comprar menos snacks.", null]
                        }
                        """, "gemini-test"))
        );

        Optional<SpendingPredictionAnalysis> analysis = analyzer.analyze(prediction());

        assertTrue(analysis.isPresent());
        assertTrue(analysis.orElseThrow().alerts().isEmpty());
        assertEquals(1, analysis.orElseThrow().recommendations().size());
        assertEquals("Comprar menos snacks.", analysis.orElseThrow().recommendations().get(0));
    }

    private SpendingPrediction prediction() {
        return new SpendingPrediction(
                UUID.randomUUID(),
                BudgetPeriod.MENSUAL,
                LocalDate.of(2026, 5, 21),
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31),
                new BigDecimal("50.00"),
                new BigDecimal("120.00"),
                new BigDecimal("10.00"),
                new BigDecimal("150.00"),
                new BigDecimal("80.00"),
                new BigDecimal("0.8000"),
                60,
                7,
                List.of(new ConsumptionCategory("Bebidas", new BigDecimal("40.00")))
        );
    }
}
