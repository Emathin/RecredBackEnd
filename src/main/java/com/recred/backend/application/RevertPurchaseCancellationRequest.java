package com.recred.backend.application;

import com.recred.backend.domain.models.RolUsuario;
import java.util.UUID;

public record RevertPurchaseCancellationRequest(
        UUID purchaseId,
        UUID requestedById,
        RolUsuario requestedByRole
) {

    public RevertPurchaseCancellationRequest(UUID purchaseId) {
        this(purchaseId, null, null);
    }
}
