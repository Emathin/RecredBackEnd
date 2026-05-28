package com.recred.backend.infrastructure.web;

import com.recred.backend.domain.models.PurchaseStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record PurchaseStatusResponseDTO(
        UUID purchaseId,
        UUID studentId,
        String status,
        String statusLabel,
        LocalDateTime date
) {

    public static PurchaseStatusResponseDTO fromDomain(PurchaseStatus purchaseStatus) {
        return new PurchaseStatusResponseDTO(
                purchaseStatus.purchaseId(),
                purchaseStatus.studentId(),
                purchaseStatus.status().name(),
                purchaseStatus.statusLabel(),
                purchaseStatus.date()
        );
    }
}
