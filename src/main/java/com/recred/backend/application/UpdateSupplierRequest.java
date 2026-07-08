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
}
