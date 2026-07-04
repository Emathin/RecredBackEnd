package com.recred.backend.unit.infrastructure.gemini;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recred.backend.application.RawBulkProduct;
import com.recred.backend.infrastructure.gemini.GeminiBulkStockAnalyzer;
import com.recred.backend.infrastructure.gemini.GeminiTextClient;
import com.recred.backend.infrastructure.gemini.GeminiTextResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeminiBulkStockAnalyzerTest {

    @Mock
    private GeminiTextClient geminiTextClient;

    private GeminiBulkStockAnalyzer analyzer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        analyzer = new GeminiBulkStockAnalyzer(geminiTextClient, objectMapper);
    }

    @Test
    void analyze_ShouldReturnProductList_WhenGeminiRespondsCorrectly() {
        String jsonResponse = """
                {
                  "productos": [
                    {
                      "nombre": "Producto 1",
                      "descripcion": "Desc 1",
                      "precio": 100.5,
                      "peso": 500,
                      "requiere_preparacion": false,
                      "categoria_nombre": "Cat 1",
                      "stock_actual": 10,
                      "salud_etiquetas": ["Sano"],
                      "tipo_etiquetas": ["Comida"]
                    }
                  ]
                }
                """;
        
        when(geminiTextClient.generateWithFile(anyString(), any(byte[].class), anyString()))
                .thenReturn(Optional.of(new GeminiTextResponse(jsonResponse, "model")));

        List<RawBulkProduct> result = analyzer.analyze("dummy".getBytes(), "application/pdf", List.of("Cat 1"));

        assertEquals(1, result.size());
        assertEquals("Producto 1", result.get(0).nombre());
        assertEquals(10, result.get(0).stockActual());
        assertEquals("Sano", result.get(0).saludEtiquetas().get(0));
    }

    @Test
    void analyze_ShouldReturnEmptyList_WhenGeminiFails() {
        when(geminiTextClient.generateWithFile(anyString(), any(byte[].class), anyString()))
                .thenReturn(Optional.empty());

        List<RawBulkProduct> result = analyzer.analyze("dummy".getBytes(), "application/pdf", List.of());

        assertTrue(result.isEmpty());
    }

    @Test
    void analyze_ShouldCleanFencedJsonAndDefaultMissingLabelsToEmptyLists() {
        String jsonResponse = """
                ```json
                {
                  "productos": [
                    {
                      "nombre": "Producto 2",
                      "descripcion": "Desc 2",
                      "precio": 50,
                      "peso": 250,
                      "requiere_preparacion": true,
                      "categoria_nombre": "Cat 2",
                      "stock_actual": 4
                    }
                  ]
                }
                ```
                """;
        when(geminiTextClient.generateWithFile(anyString(), any(byte[].class), anyString()))
                .thenReturn(Optional.of(new GeminiTextResponse(jsonResponse, "model")));

        List<RawBulkProduct> result = analyzer.analyze(
                "dummy".getBytes(),
                "application/pdf",
                List.of("Cat 2")
        );

        assertEquals(1, result.size());
        assertEquals("Producto 2", result.get(0).nombre());
        assertTrue(result.get(0).requierePreparacion());
        assertTrue(result.get(0).saludEtiquetas().isEmpty());
        assertTrue(result.get(0).tipoEtiquetas().isEmpty());
    }

    @Test
    void analyze_ShouldReturnEmptyList_WhenProductsAreNullOrJsonIsInvalid() {
        when(geminiTextClient.generateWithFile(anyString(), any(byte[].class), anyString()))
                .thenReturn(Optional.of(new GeminiTextResponse("{\"productos\": null}", "model")))
                .thenReturn(Optional.of(new GeminiTextResponse("{", "model")));

        List<RawBulkProduct> nullProducts = analyzer.analyze(
                "dummy".getBytes(),
                "application/pdf",
                List.of()
        );
        List<RawBulkProduct> invalidJson = analyzer.analyze(
                "dummy".getBytes(),
                "application/pdf",
                List.of()
        );

        assertTrue(nullProducts.isEmpty());
        assertTrue(invalidJson.isEmpty());
    }

    @Test
    void analyze_ShouldReturnEmptyList_WhenGeminiClientThrows() {
        when(geminiTextClient.generateWithFile(anyString(), any(byte[].class), anyString()))
                .thenThrow(new RuntimeException("gemini down"));

        List<RawBulkProduct> result = analyzer.analyze(
                "dummy".getBytes(),
                "application/pdf",
                List.of("Cat 1")
        );

        assertTrue(result.isEmpty());
    }
}
