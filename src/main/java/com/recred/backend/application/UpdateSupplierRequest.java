package com.recred.backend.application;

import java.util.UUID;

public record UpdateSupplierRequest(
        UUID id,
        UUID kioskOwnerId,
        String name,
        String phone,
        String email,
        String visitDays,
        String notes
) {
    public void validate() {
        if (id == null) {
            throw new IllegalArgumentException("supplierId is required");
        }

        if (kioskOwnerId == null) {
            throw new IllegalArgumentException("kioskOwnerId is required");
        }

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Supplier name is required");
        }
    }
}
