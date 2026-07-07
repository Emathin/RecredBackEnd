package com.recred.backend.application;

import java.util.UUID;

public record GetSupplierRequest(
        UUID id,
        UUID kioskOwnerId
) {
}
