package com.recred.backend.application;

import java.util.UUID;

public record RevertPurchaseCancellationRequest(UUID purchaseId) {
}
