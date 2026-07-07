package com.recred.backend.application;

import java.util.UUID;

public record CancelPresentialPurchaseRequest(UUID purchaseId, UUID studentId) {

    public CancelPresentialPurchaseRequest(UUID purchaseId) {
        this(purchaseId, null);
    }
}
