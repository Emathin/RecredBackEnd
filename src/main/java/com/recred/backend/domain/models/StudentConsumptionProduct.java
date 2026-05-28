package com.recred.backend.domain.models;

import java.math.BigDecimal;
import java.util.UUID;

public record StudentConsumptionProduct(
        UUID productId,
        String productName,
        String categoryName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal
) {
}
