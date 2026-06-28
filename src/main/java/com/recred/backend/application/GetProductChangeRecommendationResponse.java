package com.recred.backend.application;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record GetProductChangeRecommendationResponse(
        UUID recommendationId,
        String originalProductName,
        BigDecimal originalProductPrice,
        String summary,
        List<String> alerts,
        List<String> suggestedProducts,
        String model,
        ProductSaleStatisticsDto saleStatistics
) {
    public record ProductSaleStatisticsDto(
            UUID productoId,
            String nombre,
            String categoria,
            BigDecimal precioActual,
            Long ventasPeriodo,
            BigDecimal participacionVentas,
            Integer rankingGeneral,
            Integer rankingCategoria,
            BigDecimal promedioVentasCategoria,
            BigDecimal promedioPrecioCategoria,
            BigDecimal diferenciaPrecioCategoria,
            Long diasSinVenta,
            Long clientesDistintos,
            Integer stockActual,
            BigDecimal stockPromedioCategoria
    ) {}
}
