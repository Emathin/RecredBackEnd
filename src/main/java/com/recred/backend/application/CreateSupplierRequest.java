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
}
