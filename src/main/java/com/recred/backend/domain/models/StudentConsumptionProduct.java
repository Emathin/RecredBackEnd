package com.recred.backend.domain.models;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record StudentConsumptionProduct(
        UUID productId,
        String productName,
        String categoryName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal,
        List<String> healthClassifications
) {
    public StudentConsumptionProduct(
            UUID productId,
            String productName,
            String categoryName,
            Integer quantity,
            BigDecimal unitPrice,
            BigDecimal subtotal) {
        this(productId, productName, categoryName, quantity, unitPrice, subtotal, List.of());
    }

    public List<String> healthClassificationsOrEmpty() {
        return healthClassifications == null ? List.of() : healthClassifications;
    }
}
