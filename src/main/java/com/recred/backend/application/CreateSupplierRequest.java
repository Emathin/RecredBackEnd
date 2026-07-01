package com.recred.backend.application;

import java.util.UUID;

public record CreateSupplierRequest(
        UUID kioskOwnerId,
        String name,
        String phone,
        String email,
        String visitDays,
        String notes
) {
    public void validate() {
        if (kioskOwnerId == null) {
            throw new IllegalArgumentException("kioskOwnerId is required");
        }

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Supplier name is required");
        }
    }
}
