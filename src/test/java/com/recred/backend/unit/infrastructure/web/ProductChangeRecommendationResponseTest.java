package com.recred.backend.unit.infrastructure.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recred.backend.application.GetProductChangeRecommendationResponse;
import com.recred.backend.domain.models.ProductChangeAnalysis;
import com.recred.backend.domain.models.ProductChangeRecommendation;
import com.recred.backend.domain.models.ProductRecommendationResult;
import com.recred.backend.domain.models.ProductSaleStatistics;
import com.recred.backend.infrastructure.web.ProductChangeRecommendationResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductChangeRecommendationResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void fromDomainShouldMapAnalysisAndSaleStatistics() {
        ProductChangeRecommendation recommendation = new ProductChangeRecommendation();
        recommendation.setNombre("Alfajor");
        recommendation.setSaleStatistics(statistics());
        ProductRecommendationResult result = new ProductRecommendationResult(
                recommendation,
                Optional.of(new ProductChangeAnalysis(
                        "Baja rotacion",
                        List.of("Pocas ventas"),
                        List.of("Yogur"),
                        "gemini"
                ))
        );

        JsonNode json = json(ProductChangeRecommendationResponse.fromDomain(result));

        assertEquals("Alfajor", json.get("productoOriginal").asText());
        assertEquals("Baja rotacion", json.get("resumen").asText());
        assertEquals("Pocas ventas", json.get("alertas").get(0).asText());
        assertEquals("Yogur", json.get("productosSugeridos").get(0).asText());
        assertEquals("gemini", json.get("modeloIA").asText());
        assertSaleStatistics(json.get("estadisticasVenta"));
    }

    @Test
    void fromUseCaseResponseShouldMapDtoStatisticsAndAllowNullStatistics() {
        GetProductChangeRecommendationResponse response =
                new GetProductChangeRecommendationResponse(
                        UUID.randomUUID(),
                        "Alfajor",
                        new BigDecimal("550.00"),
                        "Baja rotacion",
                        List.of("Pocas ventas"),
                        List.of("Yogur"),
                        "gemini",
                        statisticsDto()
                );
        GetProductChangeRecommendationResponse withoutStatistics =
                new GetProductChangeRecommendationResponse(
                        UUID.randomUUID(),
                        "Alfajor",
                        new BigDecimal("550.00"),
                        "Baja rotacion",
                        List.of(),
                        List.of(),
                        "gemini",
                        null
                );

        JsonNode json = json(ProductChangeRecommendationResponse.fromUseCaseResponse(response));
        JsonNode nullStatsJson = json(ProductChangeRecommendationResponse.fromUseCaseResponse(
                withoutStatistics));

        assertSaleStatistics(json.get("estadisticasVenta"));
        assertTrue(nullStatsJson.get("estadisticasVenta").isNull());
    }

    private void assertSaleStatistics(JsonNode stats) {
        assertEquals("Alfajor", stats.get("nombre").asText());
        assertEquals("Golosinas", stats.get("categoria").asText());
        assertEquals(18L, stats.get("ventasPeriodo").asLong());
        assertEquals(3, stats.get("rankingGeneral").asInt());
        assertEquals(20, stats.get("stockActual").asInt());
    }

    private ProductSaleStatistics statistics() {
        return ProductSaleStatistics.builder()
                .productoId(UUID.randomUUID())
                .nombre("Alfajor")
                .categoria("Golosinas")
                .precioActual(new BigDecimal("550.00"))
                .ventasPeriodo(18L)
                .participacionVentas(new BigDecimal("22.50"))
                .rankingGeneral(3)
                .rankingCategoria(1)
                .promedioVentasCategoria(new BigDecimal("14.00"))
                .promedioPrecioCategoria(new BigDecimal("500.00"))
                .diferenciaPrecioCategoria(new BigDecimal("50.00"))
                .diasSinVenta(2L)
                .clientesDistintos(8L)
                .stockActual(20)
                .stockPromedioCategoria(new BigDecimal("18.50"))
                .build();
    }

    private GetProductChangeRecommendationResponse.ProductSaleStatisticsDto statisticsDto() {
        ProductSaleStatistics stats = statistics();
        return new GetProductChangeRecommendationResponse.ProductSaleStatisticsDto(
                stats.getProductoId(),
                stats.getNombre(),
                stats.getCategoria(),
                stats.getPrecioActual(),
                stats.getVentasPeriodo(),
                stats.getParticipacionVentas(),
                stats.getRankingGeneral(),
                stats.getRankingCategoria(),
                stats.getPromedioVentasCategoria(),
                stats.getPromedioPrecioCategoria(),
                stats.getDiferenciaPrecioCategoria(),
                stats.getDiasSinVenta(),
                stats.getClientesDistintos(),
                stats.getStockActual(),
                stats.getStockPromedioCategoria()
        );
    }

    private JsonNode json(ProductChangeRecommendationResponse response) {
        return objectMapper.valueToTree(response);
    }
}
