package com.recred.backend.domain.models;

import java.time.LocalDateTime;
import java.util.UUID;

public record PurchaseStatus(
        UUID purchaseId,
        UUID studentId,
        EstadoCompra status,
        String statusLabel,
        LocalDateTime date
) {

    public static PurchaseStatus from(Purchase purchase) {
        return new PurchaseStatus(
                purchase.getId(),
                purchase.getStudent().getId(),
                purchase.getStatus(),
                purchase.getStatus().getLabel(),
                purchase.getDate()
        );
    }
}
