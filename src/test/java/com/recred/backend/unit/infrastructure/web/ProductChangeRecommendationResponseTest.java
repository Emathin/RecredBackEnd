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
        ProductRecommendationResult result = givenDomainResult();

        JsonNode json = whenMappingFromDomain(result);

        thenDomainDataIsMapped(json);
        thenSaleStatisticsAreMapped(json.get("estadisticasVenta"));
    }

    @Test
    void fromUseCaseResponseShouldMapDtoStatisticsAndAllowNullStatistics() {
        GetProductChangeRecommendationResponse response = givenUseCaseResponseWithStatistics();
        GetProductChangeRecommendationResponse withoutStatistics = givenUseCaseResponseWithoutStatistics();

        JsonNode json = whenMappingFromUseCaseResponse(response);
        JsonNode nullStatsJson = whenMappingFromUseCaseResponse(withoutStatistics);

        thenSaleStatisticsAreMapped(json.get("estadisticasVenta"));
        thenStatisticsAreNull(nullStatsJson);
    }

    private ProductRecommendationResult givenDomainResult() {
        ProductChangeRecommendation recommendation = new ProductChangeRecommendation();
        recommendation.setNombre("Alfajor");
        recommendation.setSaleStatistics(statistics());
        return new ProductRecommendationResult(
                recommendation,
                Optional.of(new ProductChangeAnalysis(
                        "Baja rotacion",
                        List.of("Pocas ventas"),
                        List.of("Yogur"),
                        "gemini"
                ))
        );
    }

    private GetProductChangeRecommendationResponse givenUseCaseResponseWithStatistics() {
        return new GetProductChangeRecommendationResponse(
                UUID.randomUUID(),
                "Alfajor",
                new BigDecimal("550.00"),
                "Baja rotacion",
                List.of("Pocas ventas"),
                List.of("Yogur"),
                "gemini",
                statisticsDto()
        );
    }

    private GetProductChangeRecommendationResponse givenUseCaseResponseWithoutStatistics() {
        return new GetProductChangeRecommendationResponse(
                UUID.randomUUID(),
                "Alfajor",
                new BigDecimal("550.00"),
                "Baja rotacion",
                List.of(),
                List.of(),
                "gemini",
                null
        );
    }

    private JsonNode whenMappingFromDomain(ProductRecommendationResult result) {
        return json(ProductChangeRecommendationResponse.fromDomain(result));
    }

    private JsonNode whenMappingFromUseCaseResponse(GetProductChangeRecommendationResponse response) {
        return json(ProductChangeRecommendationResponse.fromUseCaseResponse(response));
    }

    private void thenDomainDataIsMapped(JsonNode json) {
        assertEquals("Alfajor", json.get("productoOriginal").asText());
        assertEquals("Baja rotacion", json.get("resumen").asText());
        assertEquals("Pocas ventas", json.get("alertas").get(0).asText());
        assertEquals("Yogur", json.get("productosSugeridos").get(0).asText());
        assertEquals("gemini", json.get("modeloIA").asText());
    }

    private void thenSaleStatisticsAreMapped(JsonNode stats) {
        assertEquals("Alfajor", stats.get("nombre").asText());
        assertEquals("Golosinas", stats.get("categoria").asText());
        assertEquals(18L, stats.get("ventasPeriodo").asLong());
        assertEquals(3, stats.get("rankingGeneral").asInt());
        assertEquals(20, stats.get("stockActual").asInt());
    }

    private void thenStatisticsAreNull(JsonNode json) {
        assertTrue(json.get("estadisticasVenta").isNull());
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
