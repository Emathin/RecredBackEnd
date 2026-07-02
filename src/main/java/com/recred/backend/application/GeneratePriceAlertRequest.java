package com.recred.backend.application;

import com.recred.backend.domain.models.Producto;

import java.math.BigDecimal;
import java.util.UUID;

public record GeneratePriceAlertRequest(
        Producto product,
        BigDecimal oldPrice,
        BigDecimal newPrice,
        UUID buffetId
) {

    public void validate() {
        validateProduct();
        validatePrices();
    }

    private void validateProduct() {
        if (product == null) {
            throw new IllegalArgumentException("El producto es obligatorio");
        }
        if (product.getId() == null) {
            throw new IllegalArgumentException("El id del producto es obligatorio");
        }
    }

    private void validatePrices() {
        if (oldPrice == null) {
            throw new IllegalArgumentException("El precio anterior es obligatorio");
        }
        if (newPrice == null) {
            throw new IllegalArgumentException("El precio nuevo es obligatorio");
        }
    }
}
