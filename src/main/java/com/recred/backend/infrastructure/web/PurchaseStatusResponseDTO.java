package com.recred.backend.infrastructure.web;

import com.recred.backend.application.GetPurchaseStatusResponse;

import java.time.LocalDateTime;
import java.util.UUID;

public record PurchaseStatusResponseDTO(
        UUID purchaseId,
        UUID studentId,
        String status,
        String statusLabel,
        LocalDateTime date
) {

    public static PurchaseStatusResponseDTO fromApplication(
            GetPurchaseStatusResponse purchaseStatus
    ) {
        return new PurchaseStatusResponseDTO(
                purchaseStatus.purchaseId(),
                purchaseStatus.studentId(),
                purchaseStatus.status(),
                purchaseStatus.statusLabel(),
                purchaseStatus.date()
        );
    }
}
