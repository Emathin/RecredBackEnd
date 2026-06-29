package com.recred.backend.application;

import com.recred.backend.domain.models.ClasificacionSalud;
import com.recred.backend.domain.models.ClasificacionTipo;
import com.recred.backend.domain.models.Producto;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public record ListInventoryProductsResponse(List<InventoryProductResponse> products) {

    public static ListInventoryProductsResponse fromDomain(List<Producto> products) {
        return new ListInventoryProductsResponse(products.stream()
                .map(InventoryProductResponse::fromDomain)
                .toList());
    }

    public record InventoryProductResponse(
            UUID id,
            String nombre,
            String descripcion,
            BigDecimal precio,
            BigDecimal peso,
            Boolean requierePreparacion,
            Boolean activo,
            LocalDateTime creadoEn,
            LocalDateTime actualizadoEn,
            Integer stockActual,
            Integer stockReservado,
            Integer stockMinimo,
            Integer stockDisponible,
            String estadoInventario,
            String tipoManejoInventario,
            Integer cupoMaximoDiario,
            Integer cupoDisponibleDia,
            String urlImagen,
            CategoryResponse categoria,
            Set<ClassificationResponse> clasificacionesSalud,
            Set<ClassificationResponse> tipos,
            String inventoryMode,
            Integer effectiveDailyCapacity,
            Integer effectiveDailyCapacityAvailable,
            Boolean availableForSale,
            Boolean lowStock,
            Boolean soldOut
    ) {
        private static InventoryProductResponse fromDomain(Producto product) {
            return new InventoryProductResponse(
                    product.getId(),
                    product.getNombre(),
                    product.getDescripcion(),
                    product.getPrecio(),
                    product.getPeso(),
                    product.getRequierePreparacion(),
                    product.getActivo(),
                    product.getCreadoEn(),
                    product.getActualizadoEn(),
                    product.getStockActual(),
                    product.getStockReservado(),
                    product.getStockMinimo(),
                    product.getStockDisponible(),
                    product.getEstadoInventario() == null
                            ? null
                            : product.getEstadoInventario().name(),
                    product.getTipoManejoInventario() == null
                            ? null
                            : product.getTipoManejoInventario().name(),
                    product.getCupoMaximoDiario(),
                    product.getCupoDisponibleDia(),
                    product.getUrlImagen(),
                    CategoryResponse.fromDomain(product),
                    ClassificationResponse.fromHealthClassifications(
                            product.getClasificacionesSalud()),
                    ClassificationResponse.fromTypeClassifications(product.getTipos()),
                    product.inventoryMode().name(),
                    product.configuredDailyCapacity(),
                    product.dailyCapacityAvailable(),
                    product.isAvailableForSale(),
                    product.hasLowStockStatus(),
                    product.isSoldOut()
            );
        }
    }

    public record CategoryResponse(UUID id, String descripcion) {
        private static CategoryResponse fromDomain(Producto product) {
            if (product.getCategoria() == null) {
                return null;
            }

            return new CategoryResponse(
                    product.getCategoria().getId(),
                    product.getCategoria().getDescripcion()
            );
        }
    }

    public record ClassificationResponse(UUID id, String descripcion) {
        private static Set<ClassificationResponse> fromHealthClassifications(
                Set<ClasificacionSalud> classifications
        ) {
            if (classifications == null) {
                return Set.of();
            }

            return classifications.stream()
                    .map(classification -> new ClassificationResponse(
                            classification.getId(),
                            classification.getDescripcion()))
                    .collect(Collectors.toSet());
        }

        private static Set<ClassificationResponse> fromTypeClassifications(
                Set<ClasificacionTipo> classifications
        ) {
            if (classifications == null) {
                return Set.of();
            }

            return classifications.stream()
                    .map(classification -> new ClassificationResponse(
                            classification.getId(),
                            classification.getDescripcion()))
                    .collect(Collectors.toSet());
        }
    }
}
