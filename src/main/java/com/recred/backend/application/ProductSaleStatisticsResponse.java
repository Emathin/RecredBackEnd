package com.recred.backend.application;

import com.recred.backend.domain.models.ProductSaleStatistics;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductSaleStatisticsResponse(
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
) {

    public static ProductSaleStatisticsResponse fromDomain(ProductSaleStatistics statistics) {
        return new ProductSaleStatisticsResponse(
                statistics.getProductoId(),
                statistics.getNombre(),
                statistics.getCategoria(),
                statistics.getPrecioActual(),
                statistics.getVentasPeriodo(),
                statistics.getParticipacionVentas(),
                statistics.getRankingGeneral(),
                statistics.getRankingCategoria(),
                statistics.getPromedioVentasCategoria(),
                statistics.getPromedioPrecioCategoria(),
                statistics.getDiferenciaPrecioCategoria(),
                statistics.getDiasSinVenta(),
                statistics.getClientesDistintos(),
                statistics.getStockActual(),
                statistics.getStockPromedioCategoria()
        );
    }
}
