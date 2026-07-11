package com.recred.backend.application;

import com.recred.backend.domain.models.RolUsuario;
import java.util.UUID;

public record CancelPresentialPurchaseRequest(
        UUID purchaseId, 
        UUID studentId,
        UUID cancelledById,
        RolUsuario cancelledByRole
) {

    public CancelPresentialPurchaseRequest(UUID purchaseId) {
        this(purchaseId, null, null, null);
    }

    public CancelPresentialPurchaseRequest(UUID purchaseId, UUID studentId) {
        this(purchaseId, studentId, null, null);
    }
}
