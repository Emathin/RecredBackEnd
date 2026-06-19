package com.recred.backend.web;

import com.recred.backend.domain.models.ProductChangeAnalysis;
import com.recred.backend.domain.models.ProductChangeRecommendation;
import com.recred.backend.domain.models.ProductRecommendationResult;
import com.recred.backend.domain.models.ProductSaleStatistics;
import com.recred.backend.infrastructure.web.ProductChangeRecommendationResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ProductChangeRecommendationResponseTest {

    @Test
    void fromDomain_WithAnalysisAndStatistics_ShouldMapAllFields() {
        UUID productId = UUID.randomUUID();
        ProductRecommendationResult result = result(
                Optional.of(new ProductChangeAnalysis(
                        "Resumen",
                        List.of("Alerta"),
                        List.of("Recomendacion"),
                        "gemini-test"
                )),
                statistics(productId)
        );

        ProductChangeRecommendationResponse response =
                ProductChangeRecommendationResponse.fromDomain(result);

        assertEquals("Alfajor", response.productoOriginal());
        assertEquals("Resumen", response.resumen());
        assertEquals(List.of("Alerta"), response.alertas());
        assertEquals(List.of("Recomendacion"), response.productosSugeridos());
        assertEquals("gemini-test", response.modeloIA());
        assertEquals(productId, response.estadisticasVenta().productoId());
        assertEquals(new BigDecimal("100.00"),
                response.estadisticasVenta().precioActual());
    }

    @Test
    void fromDomain_WithoutAnalysisOrStatistics_ShouldUseEmptyDefaults() {
        ProductRecommendationResult result = result(Optional.empty(), null);

        ProductChangeRecommendationResponse response =
                ProductChangeRecommendationResponse.fromDomain(result);

        assertNull(response.resumen());
        assertEquals(List.of(), response.alertas());
        assertEquals(List.of(), response.productosSugeridos());
        assertNull(response.modeloIA());
        assertNull(response.estadisticasVenta());
    }

    private ProductRecommendationResult result(
            Optional<ProductChangeAnalysis> analysis,
            ProductSaleStatistics statistics
    ) {
        ProductChangeRecommendation recommendation = new ProductChangeRecommendation();
        recommendation.setNombre("Alfajor");
        recommendation.setSaleStatistics(statistics);
        return new ProductRecommendationResult(recommendation, analysis);
    }

    private ProductSaleStatistics statistics(UUID productId) {
        return ProductSaleStatistics.builder()
                .productoId(productId)
                .nombre("Alfajor")
                .categoria("Snacks")
                .precioActual(new BigDecimal("100.00"))
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
                .build();
    }
}
