package com.recred.backend.domain.events;

import com.recred.backend.domain.models.EstadoInventario;
import com.recred.backend.domain.models.Inventario;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.TipoMovimientoStock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record KiosqueroRealtimeEvent(
        UUID buffetId,
        KiosqueroRealtimeEventType type,
        KiosqueroRealtimeChangeKind changeKind,
        String reason,
        LocalDate date,
        LocalDateTime occurredAt,
        String message,
        KiosqueroRealtimePayload payload
) {
    public KiosqueroRealtimeEvent {
        occurredAt = occurredAt == null ? LocalDateTime.now() : occurredAt;
        date = date == null ? occurredAt.toLocalDate() : date;
        payload = payload == null ? new EmptyRealtimePayload() : payload;
    }

    public static KiosqueroRealtimeEvent inventory(
            KiosqueroRealtimeEventType type,
            Inventario inventory,
            TipoMovimientoStock movementType,
            KiosqueroRealtimeChangeKind changeKind,
            String reason,
            String message
    ) {
        EstadoInventario currentStatus = inventory.estadoActual();
        InventoryRealtimePayload payload = new InventoryRealtimePayload(
                inventory.getProductoId(),
                inventory.getId(),
                movementType,
                inventory.getStockActual(),
                inventory.getStockReservado(),
                inventory.stockDisponible(),
                inventory.stockMinimoEfectivo(),
                inventory.getCupoMaximoDiario(),
                inventory.dailyCapacityAvailable(),
                inventory.isAvailableForSale(),
                currentStatus,
                inventory.getTipoManejo()
        );
        return new KiosqueroRealtimeEvent(
                inventory.getBuffetId(),
                type,
                changeKind,
                reason,
                null,
                null,
                message,
                payload
        );
    }

    public static KiosqueroRealtimeEvent purchase(
            KiosqueroRealtimeEventType type,
            Purchase purchase,
            String message
    ) {
        PurchaseRealtimePayload payload = new PurchaseRealtimePayload(
                purchase.getId(),
                purchase.getStatus(),
                purchase.getTotalAmount()
        );
        return new KiosqueroRealtimeEvent(
                purchase.getBuffetId(),
                type,
                KiosqueroRealtimeChangeKind.ORDER_CHANGED,
                message,
                purchase.getDate() == null ? null : purchase.getDate().toLocalDate(),
                purchase.getDate(),
                message,
                payload
        );
    }

    public static KiosqueroRealtimeEvent productAlert(
            UUID buffetId,
            KiosqueroRealtimeEventType type,
            UUID productId,
            String message
    ) {
        return new KiosqueroRealtimeEvent(
                buffetId,
                type,
                null,
                null,
                null,
                null,
                message,
                new ProductAlertRealtimePayload(productId)
        );
    }

    public static KiosqueroRealtimeEvent dashboardChanged(
            UUID buffetId,
            LocalDate date,
            KiosqueroRealtimeChangeKind changeKind,
            String reason
    ) {
        return new KiosqueroRealtimeEvent(
                buffetId,
                KiosqueroRealtimeEventType.DASHBOARD_CHANGED,
                changeKind,
                reason,
                date,
                null,
                "Dashboard actualizado",
                new EmptyRealtimePayload()
        );
    }

    public static KiosqueroRealtimeEvent dailyReportChanged(
            UUID buffetId,
            LocalDate date,
            String reason
    ) {
        return new KiosqueroRealtimeEvent(
                buffetId,
                KiosqueroRealtimeEventType.DAILY_REPORT_CHANGED,
                KiosqueroRealtimeChangeKind.DAILY_REPORT_CHANGED,
                reason,
                date,
                null,
                "Reporte diario actualizado",
                new EmptyRealtimePayload()
        );
    }
}
