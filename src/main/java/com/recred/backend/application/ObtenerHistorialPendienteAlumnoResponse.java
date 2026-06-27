package com.recred.backend.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record ObtenerHistorialPendienteAlumnoResponse(
        UUID id,
        UUID studentId,
        BigDecimal totalAmount,
        String status,
        String statusLabel,
        String paymentMethod,
        LocalDateTime date,
        List<Item> items,
        String tipo,
        String withdrawalCode,
        String pickupSlotDescription,
        LocalDate pickupDate,
        LocalTime pickupSlotStartTime,
        String withdrawalStatus
) {
    public record Item(
            UUID productId,
            String nombre,
            Integer quantity,
            BigDecimal precio,
            Boolean requierePreparacion,
            Integer cupoMaximoDiario,
            UUID promocionId,
            String promocionNombre
    ) {}
}
