package com.recred.backend.unit.infrastructure.gemini;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recred.backend.application.RawSupplierPriceItem;
import com.recred.backend.infrastructure.gemini.GeminiSupplierPriceListAnalyzer;
import com.recred.backend.infrastructure.gemini.GeminiTextClient;
import com.recred.backend.infrastructure.gemini.GeminiTextResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeminiSupplierPriceListAnalyzerTest {

    @Mock
    private GeminiTextClient geminiTextClient;

    private GeminiSupplierPriceListAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        analyzer = new GeminiSupplierPriceListAnalyzer(
                geminiTextClient,
                new ObjectMapper()
        );
    }

    @Test
    void analyzeShouldParseMarkdownWrappedJsonResponse() {
        String response = """
                ```json
                {
                  "items": [
                    {
                      "nombre_producto": "Alfajor",
                      "precio": 350.50,
                      "unidad": "caja x 12",
                      "notas": "promo"
                    }
                  ]
                }
                ```
                """;
        when(geminiTextClient.generateWithFile(anyString(), any(byte[].class), anyString()))
                .thenReturn(Optional.of(new GeminiTextResponse(response, "gemini")));

        List<RawSupplierPriceItem> result = analyzer.analyze(
                "file".getBytes(),
                "application/pdf"
        );

        assertEquals(1, result.size());
        assertEquals("Alfajor", result.get(0).nombreProducto());
        assertEquals(new BigDecimal("350.50"), result.get(0).precio());
        assertEquals("caja x 12", result.get(0).unidad());
        assertEquals("promo", result.get(0).notas());
    }

    @Test
    void analyzeShouldReturnEmptyWhenGeminiHasNoResponseOrThrows() {
        when(geminiTextClient.generateWithFile(anyString(), any(byte[].class), anyString()))
                .thenReturn(Optional.empty())
                .thenThrow(new RuntimeException("Gemini unavailable"));

        assertTrue(analyzer.analyze("file".getBytes(), "text/csv").isEmpty());
        assertTrue(analyzer.analyze("file".getBytes(), "text/csv").isEmpty());
    }

    @Test
    void analyzeShouldReturnEmptyWhenItemsAreNullOrJsonIsInvalid() {
        when(geminiTextClient.generateWithFile(anyString(), any(byte[].class), anyString()))
                .thenReturn(Optional.of(new GeminiTextResponse("{\"items\": null}", "gemini")))
                .thenReturn(Optional.of(new GeminiTextResponse("not-json", "gemini")));

        assertTrue(analyzer.analyze("file".getBytes(), "text/csv").isEmpty());
        assertTrue(analyzer.analyze("file".getBytes(), "text/csv").isEmpty());
    }
}
