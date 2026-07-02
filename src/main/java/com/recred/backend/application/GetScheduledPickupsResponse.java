package com.recred.backend.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record GetScheduledPickupsResponse(
        UUID id,
        UUID studentId,
        String studentName,
        BigDecimal totalAmount,
        String status,
        String statusLabel,
        String withdrawalStatus,
        String paymentMethod,
        LocalDateTime date,
        String withdrawalCode,
        UUID pickupSlotId,
        String pickupSlotDescription,
        LocalDate pickupDate,
        List<PurchaseItemResponse> items
) {
    public record PurchaseItemResponse(
            UUID productId,
            String name,
            Integer quantity,
            BigDecimal price
    ) {}
}
