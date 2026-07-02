package com.recred.backend.application;

import com.recred.backend.domain.models.Purchase;
import java.time.LocalDateTime;
import java.util.UUID;

public record GetPurchaseStatusResponse(
        UUID purchaseId,
        UUID studentId,
        String status,
        String statusLabel,
        LocalDateTime date
) {

    public static GetPurchaseStatusResponse from(Purchase purchase) {
        return new GetPurchaseStatusResponse(
                purchase.getId(),
                purchase.studentIdOrNull(),
                purchase.statusName(),
                purchase.statusLabel(),
                purchase.getDate()
        );
    }
}
