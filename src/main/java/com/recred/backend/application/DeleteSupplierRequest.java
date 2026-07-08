package com.recred.backend.application;

import java.util.UUID;

public record DeleteSupplierRequest(
        UUID id,
        UUID kioskOwnerId
) {
}
