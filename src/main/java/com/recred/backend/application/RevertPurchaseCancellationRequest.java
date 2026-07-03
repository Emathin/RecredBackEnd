package com.recred.backend.application;

import java.util.UUID;

public record RevertPurchaseCancellationRequest(UUID purchaseId) {

    public void validate() {
        if (purchaseId == null) {
            throw new IllegalArgumentException("purchaseId cannot be null");
        }
    }
}
