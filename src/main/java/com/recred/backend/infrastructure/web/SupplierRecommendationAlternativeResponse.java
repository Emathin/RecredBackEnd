package com.recred.backend.infrastructure.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.recred.backend.application.RecommendSupplierForProductsResponse;
import java.math.BigDecimal;
import java.util.UUID;

public record SupplierRecommendationAlternativeResponse(
        @JsonProperty("proveedorId")
        UUID supplierId,

        @JsonProperty("nombreProveedor")
        String supplierName,

        @JsonProperty("precio")
        BigDecimal price,

        @JsonProperty("unidad")
        String unit,

        @JsonProperty("precioUnitario")
        BigDecimal unitPrice
) {
    public static SupplierRecommendationAlternativeResponse fromApplication(
            RecommendSupplierForProductsResponse.Alternative response
    ) {
        return new SupplierRecommendationAlternativeResponse(
                response.supplierId(),
                response.supplierName(),
                response.price(),
                response.unit(),
                response.unitPrice()
        );
    }
}
