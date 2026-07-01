package com.recred.backend.application;

import java.util.UUID;

public record ListSuppliersRequest(UUID kioskOwnerId) {
    public void validate() {
        if (kioskOwnerId == null) {
            throw new IllegalArgumentException("kioskOwnerId is required");
        }
    }
}
