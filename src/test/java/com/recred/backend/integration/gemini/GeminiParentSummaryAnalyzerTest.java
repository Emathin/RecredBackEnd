package com.recred.backend.infrastructure.gemini;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GeminiParentSummaryAnalyzerTest {

    @Test
    void analyze_WhenGeminiGeneratesResponse_ShouldReturnText() {
        GeminiTextClient geminiTextClient = mock(GeminiTextClient.class);
        GeminiParentSummaryAnalyzer analyzer = new GeminiParentSummaryAnalyzer(geminiTextClient);
        when(geminiTextClient.generate(contains("Resumen actual: gasto semanal")))
                .thenReturn(Optional.of(new GeminiTextResponse("Analisis generado", "model")));

        String result = analyzer.analyze("gasto semanal");

        assertEquals("Analisis generado", result);
    }

    @Test
    void analyze_WhenGeminiDoesNotGenerateResponse_ShouldReturnFallback() {
        GeminiTextClient geminiTextClient = mock(GeminiTextClient.class);
        GeminiParentSummaryAnalyzer analyzer = new GeminiParentSummaryAnalyzer(geminiTextClient);
        when(geminiTextClient.generate(contains("Resumen actual: gasto semanal")))
                .thenReturn(Optional.empty());

        String result = analyzer.analyze("gasto semanal");

        assertTrue(result.startsWith("No se pudo generar"));
    }
}
