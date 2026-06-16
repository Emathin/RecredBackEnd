package com.recred.backend.infrastructure.gemini;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recred.backend.domain.models.ProductChangeAnalysis;
import com.recred.backend.domain.models.ProductChangeRecommendation;
import com.recred.backend.domain.models.ProductSaleStatistics;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeminiproductChangeAnalyzerTest {

    @Mock
    private GeminiTextClient geminiTextClient;

    @Test
    void analyze_WhenGeminiReturnsValidJson_ShouldParseAnalysis() {
        GeminiproductChangeAnalyzer analyzer = analyzer();
        when(geminiTextClient.generate(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(Optional.of(new GeminiTextResponse("""
                        {
                          "resumen": "Cambiar por opcion mas saludable.",
                          "alertas": ["Precio alto", "", null],
                          "recomendaciones": ["Probar cereal"]
                        }
                        """, "gemini-test")));

        Optional<ProductChangeAnalysis> result = analyzer.analyze(recommendation());

        assertTrue(result.isPresent());
        assertEquals("Cambiar por opcion mas saludable.", result.orElseThrow().summary());
        assertEquals(1, result.orElseThrow().alerts().size());
        assertEquals("Probar cereal", result.orElseThrow().recommendations().get(0));
        assertEquals("gemini-test", result.orElseThrow().model());
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(geminiTextClient).generate(promptCaptor.capture());
        assertTrue(promptCaptor.getValue().contains("nombreProducto=Alfajor"));
    }

    @Test
    void analyze_WhenGeminiReturnsNullAlerts_ShouldMapEmptyAlerts() {
        GeminiproductChangeAnalyzer analyzer = analyzer();
        when(geminiTextClient.generate(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(Optional.of(new GeminiTextResponse("""
                        {
                          "resumen": "Sin alertas.",
                          "alertas": null,
                          "recomendaciones": []
                        }
                        """, "gemini-test")));

        Optional<ProductChangeAnalysis> result = analyzer.analyze(recommendation());

        assertTrue(result.isPresent());
        assertEquals(List.of(), result.orElseThrow().alerts());
    }

    @Test
    void analyze_WhenGeminiReturnsInvalidJson_ShouldReturnEmpty() {
        GeminiproductChangeAnalyzer analyzer = analyzer();
        when(geminiTextClient.generate(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(Optional.of(new GeminiTextResponse("no-json", "gemini-test")));

        Optional<ProductChangeAnalysis> result = analyzer.analyze(recommendation());

        assertTrue(result.isEmpty());
    }

    @Test
    void analyze_WhenGeminiReturnsEmpty_ShouldReturnEmpty() {
        GeminiproductChangeAnalyzer analyzer = analyzer();
        when(geminiTextClient.generate(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(Optional.empty());

        Optional<ProductChangeAnalysis> result = analyzer.analyze(recommendation());

        assertTrue(result.isEmpty());
    }

    private GeminiproductChangeAnalyzer analyzer() {
        return new GeminiproductChangeAnalyzer(geminiTextClient, new ObjectMapper());
    }

    private ProductChangeRecommendation recommendation() {
        ProductChangeRecommendation recommendation = new ProductChangeRecommendation();
        recommendation.setId(UUID.randomUUID());
        recommendation.setIdBuffet(UUID.randomUUID());
        recommendation.setNombre("Alfajor");
        recommendation.setPrecio(new BigDecimal("100.00"));
        recommendation.setSaleStatistics(ProductSaleStatistics.builder()
                .categoria("Snacks")
                .ventasPeriodo(12L)
                .participacionVentas(new BigDecimal("0.30"))
                .rankingGeneral(2)
                .rankingCategoria(1)
                .promedioVentasCategoria(new BigDecimal("20.00"))
                .promedioPrecioCategoria(new BigDecimal("80.00"))
                .diferenciaPrecioCategoria(new BigDecimal("20.00"))
                .diasSinVenta(3L)
                .clientesDistintos(8L)
                .stockActual(5)
                .stockPromedioCategoria(new BigDecimal("10.00"))
                .build());
        return recommendation;
    }
}
