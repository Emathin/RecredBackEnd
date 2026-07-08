package com.recred.backend.application;

import java.util.UUID;

public record UpdatePriceListItemMappingRequest(
        UUID kioskOwnerId,
        UUID itemId,
        UUID inventoryProductId
) {
    public void validate() {
        if (kioskOwnerId == null) {
            throw new IllegalArgumentException("kioskOwnerId is required");
        }

        if (itemId == null) {
            throw new IllegalArgumentException("itemId is required");
        }
    }
}
