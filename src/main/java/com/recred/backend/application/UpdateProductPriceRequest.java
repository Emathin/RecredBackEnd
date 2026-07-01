package com.recred.backend.application;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdateProductPriceRequest(
        UUID productId,
        BigDecimal newPrice,
        UUID buffetId
) {

    public void validate() {
        if (productId == null) {
            throw new IllegalArgumentException("Product id is required");
        }
        if (newPrice == null || newPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Product price must be greater than zero");
        }
    }
}
