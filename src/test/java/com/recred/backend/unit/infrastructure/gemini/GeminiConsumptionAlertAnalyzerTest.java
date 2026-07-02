package com.recred.backend.unit.infrastructure.gemini;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recred.backend.domain.models.ConsumptionAlertAnalysisRequest;
import com.recred.backend.domain.models.ConsumptionAlertMessages;
import com.recred.backend.domain.models.ConsumptionAlertType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import com.recred.backend.infrastructure.gemini.GeminiConsumptionAlertAnalyzer;
import com.recred.backend.infrastructure.gemini.GeminiTextClient;
import com.recred.backend.infrastructure.gemini.GeminiTextResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GeminiConsumptionAlertAnalyzerTest {

    private final GeminiTextClient geminiTextClient = mock(GeminiTextClient.class);
    private final GeminiConsumptionAlertAnalyzer analyzer =
            new GeminiConsumptionAlertAnalyzer(geminiTextClient, new ObjectMapper());

    @Test
    void parseGeminiJsonResponse() {
        when(geminiTextClient.generate(contains("tipo=STATISTICAL_PATTERN"))).thenReturn(
                Optional.of(new GeminiTextResponse("""
                        {
                          "titulo": "Elegir agua",
                          "tituloTutor": "Sugerencia para Juan",
                          "mensajeAlumno": "Te recomendamos tomar mas agua.",
                          "mensajeTutor": "Recomendamos sugerir a Juan tomar mas agua.",
                          "accionSugerida": "Tomar mas agua.",
                          "confianza": 0.8
                        }
                        """, "gemini-test"))
        );

        Optional<ConsumptionAlertMessages> messages = analyzer.analyze(request());

        assertTrue(messages.isPresent());
        assertEquals("Elegir agua", messages.orElseThrow().title());
        assertEquals("Sugerencia para Juan", messages.orElseThrow().tutorTitle());
        assertEquals("Te recomendamos tomar mas agua.", messages.orElseThrow()
                .studentMessage());
        assertEquals("Recomendamos sugerir a Juan tomar mas agua.", messages.orElseThrow()
                .tutorMessage());
        assertEquals("Tomar mas agua.", messages.orElseThrow().suggestedAction());
        assertEquals(new BigDecimal("0.8000"), messages.orElseThrow().confidence());
        assertEquals("gemini-test", messages.orElseThrow().model());
    }

    @Test
    void returnEmptyWhenGeminiDoesNotGenerateText() {
        when(geminiTextClient.generate(contains("tipo=STATISTICAL_PATTERN"))).thenReturn(
                Optional.empty()
        );

        Optional<ConsumptionAlertMessages> messages = analyzer.analyze(request());

        assertTrue(messages.isEmpty());
    }

    @Test
    void returnEmptyWhenGeminiReturnsInvalidJson() {
        when(geminiTextClient.generate(contains("tipo=STATISTICAL_PATTERN"))).thenReturn(
                Optional.of(new GeminiTextResponse("no es json", "gemini-test"))
        );

        Optional<ConsumptionAlertMessages> messages = analyzer.analyze(request());

        assertTrue(messages.isEmpty());
    }

    @Test
    void returnEmptyWhenRequiredTextIsMissing() {
        when(geminiTextClient.generate(contains("tipo=STATISTICAL_PATTERN"))).thenReturn(
                Optional.of(new GeminiTextResponse("""
                        {
                          "titulo": "",
                          "tituloTutor": "Sugerencia para Juan",
                          "mensajeAlumno": "Te recomendamos tomar mas agua.",
                          "mensajeTutor": "Recomendamos sugerir a Juan tomar mas agua.",
                          "accionSugerida": "Tomar mas agua.",
                          "confianza": 0.8
                        }
                        """, "gemini-test"))
        );

        Optional<ConsumptionAlertMessages> messages = analyzer.analyze(request());

        assertTrue(messages.isEmpty());
    }

    @Test
    void useDefaultConfidenceWhenGeminiDoesNotReturnIt() {
        when(geminiTextClient.generate(contains("tipo=STATISTICAL_PATTERN"))).thenReturn(
                Optional.of(new GeminiTextResponse("""
                        {
                          "titulo": "Elegir agua",
                          "tituloTutor": "Sugerencia para Juan",
                          "mensajeAlumno": "Te recomendamos tomar mas agua.",
                          "mensajeTutor": "Recomendamos sugerir a Juan tomar mas agua.",
                          "accionSugerida": "Tomar mas agua."
                        }
                        """, "gemini-test"))
        );

        Optional<ConsumptionAlertMessages> messages = analyzer.analyze(request());

        assertTrue(messages.isPresent());
        assertEquals(new BigDecimal("0.7000"), messages.orElseThrow().confidence());
    }

    @Test
    void includeMissingOptionalDataAsSinDatoInPrompt() {
        when(geminiTextClient.generate(contains("categoriaPrincipal=sin dato"))).thenReturn(
                Optional.of(new GeminiTextResponse("""
                        {
                          "titulo": "Elegir fruta",
                          "tituloTutor": "Sugerencia saludable",
                          "mensajeAlumno": "Proba sumar una fruta en el recreo.",
                          "mensajeTutor": "Podes sugerirle sumar una fruta en el recreo.",
                          "accionSugerida": "Sumar una fruta.",
                          "confianza": 0.7
                        }
                        """, "gemini-test"))
        );

        Optional<ConsumptionAlertMessages> messages = analyzer.analyze(healthyRequest());

        assertTrue(messages.isPresent());
        assertEquals("Elegir fruta", messages.orElseThrow().title());
    }

    @Test
    void includeBlankOptionalDataAsSinDatoInPrompt() {
        when(geminiTextClient.generate(contains("productoSugerido=sin dato"))).thenReturn(
                Optional.of(new GeminiTextResponse("""
                        {
                          "titulo": "Elegir agua",
                          "tituloTutor": "Sugerencia para Juan",
                          "mensajeAlumno": "Te recomendamos tomar mas agua.",
                          "mensajeTutor": "Recomendamos sugerir a Juan tomar mas agua.",
                          "accionSugerida": "Tomar mas agua.",
                          "confianza": 0.8
                        }
                        """, "gemini-test"))
        );

        Optional<ConsumptionAlertMessages> messages = analyzer.analyze(blankProductRequest());

        assertTrue(messages.isPresent());
        assertEquals("Elegir agua", messages.orElseThrow().title());
    }

    private ConsumptionAlertAnalysisRequest request() {
        return new ConsumptionAlertAnalysisRequest(
                ConsumptionAlertType.STATISTICAL_PATTERN,
                "Juan Perez",
                LocalDate.of(2026, 5, 25),
                LocalDate.of(2026, 4, 26),
                LocalDate.of(2026, 5, 25),
                4,
                new BigDecimal("100.00"),
                "Bebidas",
                new BigDecimal("80.00"),
                "Agua"
        );
    }

    private ConsumptionAlertAnalysisRequest healthyRequest() {
        return new ConsumptionAlertAnalysisRequest(
                ConsumptionAlertType.HEALTHY_RECOMMENDATION,
                "Juan Perez",
                LocalDate.of(2026, 5, 25),
                LocalDate.of(2026, 4, 26),
                LocalDate.of(2026, 5, 25),
                0,
                BigDecimal.ZERO,
                null,
                BigDecimal.ZERO,
                null
        );
    }

    private ConsumptionAlertAnalysisRequest blankProductRequest() {
        return new ConsumptionAlertAnalysisRequest(
                ConsumptionAlertType.STATISTICAL_PATTERN,
                "Juan Perez",
                LocalDate.of(2026, 5, 25),
                LocalDate.of(2026, 4, 26),
                LocalDate.of(2026, 5, 25),
                4,
                new BigDecimal("100.00"),
                "Bebidas",
                new BigDecimal("80.00"),
                " "
        );
    }
}

