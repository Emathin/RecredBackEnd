package com.recred.backend.infrastructure.web;

import com.recred.backend.domain.events.KiosqueroRealtimeEvent;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record KiosqueroRealtimeEventResponse(
        UUID buffetId,
        String type,
        UUID productId,
        UUID inventoryId,
        UUID purchaseId,
        String purchaseStatus,
        BigDecimal purchaseTotal,
        String movementType,
        Integer stockActual,
        Integer stockReservado,
        Integer stockDisponible,
        Integer stockMinimo,
        Integer cupoMaximoDiario,
        Integer cupoDisponibleDia,
        Boolean disponible,
        String estadoInventario,
        String tipoManejoInventario,
        String changeKind,
        String reason,
        java.time.LocalDate date,
        LocalDateTime occurredAt,
        String message
) {
    public static KiosqueroRealtimeEventResponse fromDomain(KiosqueroRealtimeEvent event) {
        return new KiosqueroRealtimeEventResponse(
                event.buffetId(),
                event.type().name(),
                event.productId(),
                event.inventoryId(),
                event.purchaseId(),
                event.purchaseStatus() == null ? null : event.purchaseStatus().name(),
                event.purchaseTotal(),
                event.movementType() == null ? null : event.movementType().name(),
                event.stockActual(),
                event.stockReservado(),
                event.stockDisponible(),
                event.stockMinimo(),
                event.cupoMaximoDiario(),
                event.cupoDisponibleDia(),
                event.disponible(),
                event.estadoInventario() == null ? null : event.estadoInventario().name(),
                event.tipoManejoInventario() == null
                        ? null
                        : event.tipoManejoInventario().name(),
                event.changeKind() == null ? null : event.changeKind().name(),
                event.reason(),
                event.date(),
                event.occurredAt(),
                event.message()
        );
    }
}
