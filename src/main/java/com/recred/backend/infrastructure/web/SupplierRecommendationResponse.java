package com.recred.backend.infrastructure.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.recred.backend.application.RecommendSupplierForProductsResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record SupplierRecommendationResponse(
        @JsonProperty("productoInventarioId")
        UUID inventoryProductId,

        @JsonProperty("nombreProducto")
        String productName,

        @JsonProperty("proveedorRecomendadoId")
        UUID recommendedSupplierId,

        @JsonProperty("nombreProveedorRecomendado")
        String recommendedSupplierName,

        @JsonProperty("mejorPrecio")
        BigDecimal bestPrice,

        @JsonProperty("unidad")
        String unit,

        @JsonProperty("mejorPrecioUnitario")
        BigDecimal bestUnitPrice,

        @JsonProperty("alternativas")
        List<SupplierRecommendationAlternativeResponse> alternatives
) {
    public static SupplierRecommendationResponse fromApplication(
            RecommendSupplierForProductsResponse.Recommendation response
    ) {
        return new SupplierRecommendationResponse(
                response.inventoryProductId(),
                response.productName(),
                response.recommendedSupplierId(),
                response.recommendedSupplierName(),
                response.bestPrice(),
                response.unit(),
                response.bestUnitPrice(),
                response.alternatives() == null ? List.of() : response.alternatives()
                        .stream()
                        .map(SupplierRecommendationAlternativeResponse::fromApplication)
                        .toList()
        );
    }
}
