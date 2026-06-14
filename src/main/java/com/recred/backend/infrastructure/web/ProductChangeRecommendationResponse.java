package com.recred.backend.infrastructure.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.recred.backend.domain.models.ProductChangeAnalysis;
import com.recred.backend.domain.models.ProductRecommendationResult;
import com.recred.backend.domain.models.ProductSaleStatistics;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ProductChangeRecommendationResponse(
        @JsonProperty("productoOriginal")
        String productoOriginal,
        @JsonProperty("resumen")
        String resumen,
        @JsonProperty("alertas")
        List<String> alertas,
        @JsonProperty("productosSugeridos")
        List<String> productosSugeridos,
        @JsonProperty("motivoIA")
        String motivoIA,
        @JsonProperty("modeloIA")
        String modeloIA,
        @JsonProperty("estadisticasVenta")
        ProductSaleStatisticsResponse estadisticasVenta
) {
    public static ProductChangeRecommendationResponse fromDomain(
            ProductRecommendationResult result) {
        ProductChangeAnalysis analysis = result.getAiAnalysis().orElse(null);
        ProductSaleStatistics stats = result.getProductChangeRecommendation().getSaleStatistics();

        return new ProductChangeRecommendationResponse(
                result.getProductChangeRecommendation().getNombre(),
                analysis != null ? analysis.summary() : null,
                analysis != null ? analysis.alerts() : List.of(),
                analysis != null ? analysis.recommendations() : List.of(),
                "Producto dentro del top 5 menos vendidos",
                analysis != null ? analysis.model() : null,
                stats != null ? ProductSaleStatisticsResponse.fromDomain(stats) : null
        );
    }
}

record ProductSaleStatisticsResponse(
        @JsonProperty("productoId") UUID productoId,
        @JsonProperty("nombre") String nombre,
        @JsonProperty("categoria") String categoria,
        @JsonProperty("precioActual") BigDecimal precioActual,
        @JsonProperty("ventasPeriodo") Long ventasPeriodo,
        @JsonProperty("participacionVentas") BigDecimal participacionVentas,
        @JsonProperty("rankingGeneral") Integer rankingGeneral,
        @JsonProperty("rankingCategoria") Integer rankingCategoria,
        @JsonProperty("promedioVentasCategoria") BigDecimal promedioVentasCategoria,
        @JsonProperty("promedioPrecioCategoria") BigDecimal promedioPrecioCategoria,
        @JsonProperty("diferenciaPrecioCategoria") BigDecimal diferenciaPrecioCategoria,
        @JsonProperty("diasSinVenta") Long diasSinVenta,
        @JsonProperty("clientesDistintos") Long clientesDistintos,
        @JsonProperty("stockActual") Integer stockActual,
        @JsonProperty("stockPromedioCategoria") BigDecimal stockPromedioCategoria
) {
    public static ProductSaleStatisticsResponse fromDomain(ProductSaleStatistics domain) {
        return new ProductSaleStatisticsResponse(
                domain.getProductoId(),
                domain.getNombre(),
                domain.getCategoria(),
                domain.getPrecioActual(),
                domain.getVentasPeriodo(),
                domain.getParticipacionVentas(),
                domain.getRankingGeneral(),
                domain.getRankingCategoria(),
                domain.getPromedioVentasCategoria(),
                domain.getPromedioPrecioCategoria(),
                domain.getDiferenciaPrecioCategoria(),
                domain.getDiasSinVenta(),
                domain.getClientesDistintos(),
                domain.getStockActual(),
                domain.getStockPromedioCategoria()
        );
    }
}
