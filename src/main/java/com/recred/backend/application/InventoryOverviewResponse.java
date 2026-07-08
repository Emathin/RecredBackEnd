package com.recred.backend.application;

import com.recred.backend.application.ListInventoryProductsResponse.InventoryProductResponse;
import java.math.BigDecimal;
import java.util.UUID;

public record InventoryOverviewResponse(
        UUID productId,
        String nombre,
        BigDecimal precio,
        String tipoManejoInventario,
        String estadoInventario,
        Integer stockActual,
        Integer stockReservado,
        Integer stockDisponible,
        Integer stockMinimo,
        Integer cupoMaximoDiario,
        Integer cupoDisponibleDia,
        String urlImagen,
        String image,
        Boolean disponible,
        Boolean bajoStock,
        Boolean agotado
) {
    public static InventoryOverviewResponse fromProduct(InventoryProductResponse product) {
        return new InventoryOverviewResponse(
                product.id(),
                product.nombre(),
                product.precio(),
                product.inventoryMode(),
                product.estadoInventario(),
                product.stockActual(),
                product.stockReservado(),
                product.stockDisponible(),
                product.stockMinimo(),
                product.effectiveDailyCapacity(),
                product.effectiveDailyCapacityAvailable(),
                product.urlImagen(),
                product.urlImagen(),
                product.availableForSale(),
                product.lowStock(),
                product.soldOut()
        );
    }
}
