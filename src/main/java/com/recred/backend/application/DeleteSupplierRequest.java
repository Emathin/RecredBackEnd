package com.recred.backend.application;

import java.util.UUID;

public record DeleteSupplierRequest(
        UUID id,
        UUID kioskOwnerId
) {
    public void validate() {
        if (id == null) {
            throw new IllegalArgumentException("supplierId is required");
        }

        if (kioskOwnerId == null) {
            throw new IllegalArgumentException("kioskOwnerId is required");
        }
    }
}
