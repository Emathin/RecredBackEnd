package com.recred.backend.application;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record RecommendSupplierForProductsResponse(
        List<Recommendation> recommendations
) {
    static RecommendSupplierForProductsResponse empty() {
        return new RecommendSupplierForProductsResponse(List.of());
    }

    public record Recommendation(
            UUID inventoryProductId,
            String productName,
            UUID recommendedSupplierId,
            String recommendedSupplierName,
            BigDecimal bestPrice,
            String unit,
            BigDecimal bestUnitPrice,
            List<Alternative> alternatives
    ) {
    }

    public record Alternative(
            UUID supplierId,
            String supplierName,
            BigDecimal price,
            String unit,
            BigDecimal unitPrice
    ) {
    }
}
