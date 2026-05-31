package com.recred.backend.infrastructure.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.recred.backend.domain.models.StudentConsumptionProduct;
import java.math.BigDecimal;
import java.util.UUID;

public record StudentConsumptionProductResponse(
        @JsonProperty("productId")
        UUID productId,
        @JsonProperty("name")
        String productName,
        @JsonProperty("category")
        String categoryName,
        @JsonProperty("cantidad")
        Integer quantity,
        @JsonProperty("precioUnitario")
        BigDecimal unitPrice,
        @JsonProperty("subtotal")
        BigDecimal subtotal
) {
    public static StudentConsumptionProductResponse fromDomain(
            StudentConsumptionProduct product
    ) {
        return new StudentConsumptionProductResponse(
                product.productId(),
                product.productName(),
                product.categoryName(),
                product.quantity(),
                product.unitPrice(),
                product.subtotal()
        );
    }
}
