package com.recred.backend.application;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record RecommendSupplierForProductsRequest(
        UUID kioskOwnerId,
        List<UUID> productIds
) {
    List<UUID> productIdsOrEmpty() {
        if (productIds == null) {
            return List.of();
        }

        return productIds.stream()
                .filter(Objects::nonNull)
                .toList();
    }

    boolean hasProducts() {
        return !productIdsOrEmpty().isEmpty();
    }
}
