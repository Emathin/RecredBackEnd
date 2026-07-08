package com.recred.backend.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record GetPresentialPurchaseResponse(
        UUID id,
        UUID studentId,
        BigDecimal totalAmount,
        String status,
        String statusLabel,
        String paymentMethod,
        LocalDateTime date,
        String tipo,
        String withdrawalCode,
        String pickupSlotDescription,
        LocalDate pickupDate,
        LocalTime pickupSlotStartTime,
        List<ItemResponse> items
) {
    public record ItemResponse(
            UUID productId,
            String nombre,
            int quantity,
            BigDecimal precio,
            Boolean requierePreparacion,
            Integer cupoMaximoDiario,
            UUID promocionId,
            String promocionNombre
    ) {}
}
