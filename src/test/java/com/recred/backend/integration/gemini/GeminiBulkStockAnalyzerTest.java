package com.recred.backend.integration.gemini;

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

        List<RawBulkProduct> result = analyzer.analyze("dummy".getBytes(), "application/pdf");

        assertEquals(1, result.size());
        assertEquals("Producto 1", result.get(0).nombre());
        assertEquals(10, result.get(0).stockActual());
        assertEquals("Sano", result.get(0).saludEtiquetas().get(0));
    }

    @Test
    void analyze_ShouldReturnEmptyList_WhenGeminiFails() {
        when(geminiTextClient.generateWithFile(anyString(), any(byte[].class), anyString()))
                .thenReturn(Optional.empty());

        List<RawBulkProduct> result = analyzer.analyze("dummy".getBytes(), "application/pdf");

        assertTrue(result.isEmpty());
    }
}
