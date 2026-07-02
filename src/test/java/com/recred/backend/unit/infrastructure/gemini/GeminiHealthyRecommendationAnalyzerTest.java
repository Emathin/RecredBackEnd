package com.recred.backend.unit.infrastructure.gemini;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recred.backend.domain.models.HealthyProductOption;
import com.recred.backend.domain.models.HealthyRecommendationAnalysisRequest;
import com.recred.backend.domain.models.HealthyRecommendationMessages;
import com.recred.backend.domain.models.RecentProductConsumption;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.recred.backend.infrastructure.gemini.GeminiHealthyRecommendationAnalyzer;
import com.recred.backend.infrastructure.gemini.GeminiTextClient;
import com.recred.backend.infrastructure.gemini.GeminiTextResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GeminiHealthyRecommendationAnalyzerTest {

    private static final UUID COKE_ID = UUID.fromString(
            "10000000-0000-0000-0000-000000000001");
    private static final UUID WATER_ID = UUID.fromString(
            "10000000-0000-0000-0000-000000000002");
    private static final UUID BUFFET_ID = UUID.fromString(
            "10000000-0000-0000-0000-000000000003");

    private final GeminiTextClient geminiTextClient = mock(GeminiTextClient.class);
    private final GeminiHealthyRecommendationAnalyzer analyzer =
            new GeminiHealthyRecommendationAnalyzer(geminiTextClient, new ObjectMapper());

    @Test
    void parseGeminiJsonResponse() {
        when(geminiTextClient.generate(contains("opcionesDisponibles"))).thenReturn(
                Optional.of(new GeminiTextResponse("""
                        {
                          "recomendar": true,
                          "productoIdSugerido": "10000000-0000-0000-0000-000000000002",
                          "titulo": "Proba agua",
                          "mensajeAlumno": "Elegiste gaseosa estos dias. Proba agua mineral.",
                          "accionSugerida": "Elegir agua mineral",
                          "confianza": 0.85
                        }
                        """, "gemini-test"))
        );

        Optional<HealthyRecommendationMessages> messages = analyzer.analyze(request());

        assertTrue(messages.isPresent());
        assertEquals("Proba agua", messages.orElseThrow().title());
        assertEquals("Elegiste gaseosa estos dias. Proba agua mineral.", messages.orElseThrow()
                .studentMessage());
        assertEquals("Elegir agua mineral", messages.orElseThrow().suggestedAction());
        assertEquals(WATER_ID, messages.orElseThrow().suggestedProductId());
        assertEquals(new BigDecimal("0.8500"), messages.orElseThrow().confidence());
        assertEquals("gemini-test", messages.orElseThrow().model());
    }

    @Test
    void returnEmptyWhenGeminiDoesNotGenerateText() {
        when(geminiTextClient.generate(contains("opcionesDisponibles"))).thenReturn(
                Optional.empty()
        );

        Optional<HealthyRecommendationMessages> messages = analyzer.analyze(request());

        assertTrue(messages.isEmpty());
    }

    @Test
    void returnEmptyWhenGeminiReturnsInvalidJson() {
        when(geminiTextClient.generate(contains("opcionesDisponibles"))).thenReturn(
                Optional.of(new GeminiTextResponse("no es json", "gemini-test"))
        );

        Optional<HealthyRecommendationMessages> messages = analyzer.analyze(request());

        assertTrue(messages.isEmpty());
    }

    @Test
    void returnEmptyWhenGeminiDoesNotRecommendAnything() {
        when(geminiTextClient.generate(contains("opcionesDisponibles"))).thenReturn(
                Optional.of(new GeminiTextResponse("""
                        {
                          "recomendar": false
                        }
                        """, "gemini-test"))
        );

        Optional<HealthyRecommendationMessages> messages = analyzer.analyze(request());

        assertTrue(messages.isEmpty());
    }

    @Test
    void returnEmptyWhenRequiredDataIsMissing() {
        when(geminiTextClient.generate(contains("opcionesDisponibles"))).thenReturn(
                Optional.of(new GeminiTextResponse("""
                        {
                          "recomendar": true,
                          "titulo": "",
                          "mensajeAlumno": "Proba agua mineral.",
                          "accionSugerida": "Elegir agua mineral",
                          "confianza": 0.85
                        }
                        """, "gemini-test"))
        );

        Optional<HealthyRecommendationMessages> messages = analyzer.analyze(request());

        assertTrue(messages.isEmpty());
    }

    @Test
    void returnEmptyWhenTitleIsMissing() {
        when(geminiTextClient.generate(contains("opcionesDisponibles"))).thenReturn(
                Optional.of(new GeminiTextResponse("""
                        {
                          "recomendar": true,
                          "productoIdSugerido": "10000000-0000-0000-0000-000000000002",
                          "titulo": "",
                          "mensajeAlumno": "Proba agua mineral.",
                          "accionSugerida": "Elegir agua mineral",
                          "confianza": 0.85
                        }
                        """, "gemini-test"))
        );

        Optional<HealthyRecommendationMessages> messages = analyzer.analyze(request());

        assertTrue(messages.isEmpty());
    }

    @Test
    void returnEmptyWhenTitleIsNull() {
        when(geminiTextClient.generate(contains("opcionesDisponibles"))).thenReturn(
                Optional.of(new GeminiTextResponse("""
                        {
                          "recomendar": true,
                          "productoIdSugerido": "10000000-0000-0000-0000-000000000002",
                          "mensajeAlumno": "Proba agua mineral.",
                          "accionSugerida": "Elegir agua mineral",
                          "confianza": 0.85
                        }
                        """, "gemini-test"))
        );

        Optional<HealthyRecommendationMessages> messages = analyzer.analyze(request());

        assertTrue(messages.isEmpty());
    }

    @Test
    void returnEmptyWhenSuggestedProductIdIsMissing() {
        when(geminiTextClient.generate(contains("opcionesDisponibles"))).thenReturn(
                Optional.of(new GeminiTextResponse("""
                        {
                          "recomendar": true,
                          "titulo": "Proba agua",
                          "mensajeAlumno": "Proba agua mineral.",
                          "accionSugerida": "Elegir agua mineral",
                          "confianza": 0.85
                        }
                        """, "gemini-test"))
        );

        Optional<HealthyRecommendationMessages> messages = analyzer.analyze(request());

        assertTrue(messages.isEmpty());
    }

    @Test
    void returnEmptyWhenStudentMessageIsMissing() {
        when(geminiTextClient.generate(contains("opcionesDisponibles"))).thenReturn(
                Optional.of(new GeminiTextResponse("""
                        {
                          "recomendar": true,
                          "productoIdSugerido": "10000000-0000-0000-0000-000000000002",
                          "titulo": "Proba agua",
                          "mensajeAlumno": "",
                          "accionSugerida": "Elegir agua mineral",
                          "confianza": 0.85
                        }
                        """, "gemini-test"))
        );

        Optional<HealthyRecommendationMessages> messages = analyzer.analyze(request());

        assertTrue(messages.isEmpty());
    }

    @Test
    void returnEmptyWhenSuggestedActionIsMissing() {
        when(geminiTextClient.generate(contains("opcionesDisponibles"))).thenReturn(
                Optional.of(new GeminiTextResponse("""
                        {
                          "recomendar": true,
                          "productoIdSugerido": "10000000-0000-0000-0000-000000000002",
                          "titulo": "Proba agua",
                          "mensajeAlumno": "Proba agua mineral.",
                          "accionSugerida": "",
                          "confianza": 0.85
                        }
                        """, "gemini-test"))
        );

        Optional<HealthyRecommendationMessages> messages = analyzer.analyze(request());

        assertTrue(messages.isEmpty());
    }

    @Test
    void useDefaultConfidenceWhenGeminiDoesNotReturnIt() {
        when(geminiTextClient.generate(contains("opcionesDisponibles"))).thenReturn(
                Optional.of(new GeminiTextResponse("""
                        {
                          "recomendar": true,
                          "productoIdSugerido": "10000000-0000-0000-0000-000000000002",
                          "titulo": "Proba agua",
                          "mensajeAlumno": "Elegiste gaseosa estos dias. Proba agua mineral.",
                          "accionSugerida": "Elegir agua mineral"
                        }
                        """, "gemini-test"))
        );

        Optional<HealthyRecommendationMessages> messages = analyzer.analyze(request());

        assertTrue(messages.isPresent());
        assertEquals(new BigDecimal("0.7500"), messages.orElseThrow().confidence());
    }

    private HealthyRecommendationAnalysisRequest request() {
        return new HealthyRecommendationAnalysisRequest(
                "Juan Perez",
                LocalDate.of(2026, 5, 26),
                LocalDate.of(2026, 5, 24),
                LocalDate.of(2026, 5, 25),
                "Coca Cola",
                "Bebidas",
                2,
                2,
                List.of(new RecentProductConsumption(
                        COKE_ID,
                        "Coca Cola",
                        "Gaseosa azucarada",
                        "Bebidas",
                        BUFFET_ID,
                        LocalDateTime.parse("2026-05-25T10:00:00"),
                        1
                )),
                List.of(new HealthyProductOption(
                        WATER_ID,
                        "Agua mineral",
                        "Agua sin azucar",
                        "Bebidas"
                ))
        );
    }
}

