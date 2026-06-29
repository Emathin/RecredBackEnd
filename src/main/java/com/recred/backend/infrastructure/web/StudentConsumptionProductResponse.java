package com.recred.backend.infrastructure.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.recred.backend.application.GetLoggedUserConsumptionDetailResponse.ProductResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record StudentConsumptionProductResponse(
        @JsonProperty("productoId")
        UUID productId,
        @JsonProperty("nombre")
        String productName,
        @JsonProperty("categoria")
        String categoryName,
        @JsonProperty("cantidad")
        Integer quantity,
        @JsonProperty("precioUnitario")
        BigDecimal unitPrice,
        @JsonProperty("subtotal")
        BigDecimal subtotal,
        @JsonProperty("clasificacionesSalud")
        List<String> healthClassifications
) {
    public static StudentConsumptionProductResponse fromApplication(
            ProductResponse product
    ) {
        return new StudentConsumptionProductResponse(
                product.productId(),
                product.productName(),
                product.categoryName(),
                product.quantity(),
                product.unitPrice(),
                product.subtotal(),
                product.healthClassifications()
        );
    }
}
