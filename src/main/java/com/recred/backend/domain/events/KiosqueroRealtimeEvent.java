package com.recred.backend.domain.events;

import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.EstadoInventario;
import com.recred.backend.domain.models.Inventario;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.TipoManejoInventario;
import com.recred.backend.domain.models.TipoMovimientoStock;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record KiosqueroRealtimeEvent(
        UUID buffetId,
        KiosqueroRealtimeEventType type,
        UUID productId,
        UUID inventoryId,
        UUID purchaseId,
        EstadoCompra purchaseStatus,
        BigDecimal purchaseTotal,
        TipoMovimientoStock movementType,
        Integer stockActual,
        Integer stockReservado,
        Integer stockDisponible,
        Integer stockMinimo,
        Integer cupoMaximoDiario,
        Integer cupoDisponibleDia,
        Boolean disponible,
        EstadoInventario estadoInventario,
        TipoManejoInventario tipoManejoInventario,
        KiosqueroRealtimeChangeKind changeKind,
        String reason,
        LocalDate date,
        LocalDateTime occurredAt,
        String message
) {
    public KiosqueroRealtimeEvent {
        if (occurredAt == null) {
            occurredAt = LocalDateTime.now();
        }
        if (date == null) {
            date = occurredAt.toLocalDate();
        }
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
        return new KiosqueroRealtimeEvent(
                inventory.getBuffetId(),
                type,
                inventory.getProductoId(),
                inventory.getId(),
                null,
                null,
                null,
                movementType,
                inventory.getStockActual(),
                inventory.getStockReservado(),
                inventory.stockDisponible(),
                inventory.stockMinimoEfectivo(),
                inventory.getCupoMaximoDiario(),
                dailyCapacityAvailable(inventory),
                isAvailable(currentStatus, inventory.stockDisponible()),
                currentStatus,
                inventory.getTipoManejo(),
                changeKind,
                reason,
                null,
                null,
                message
        );
    }

    public static KiosqueroRealtimeEvent purchase(
            KiosqueroRealtimeEventType type,
            Purchase purchase,
            String message
    ) {
        return new KiosqueroRealtimeEvent(
                purchase.getBuffetId(),
                type,
                null,
                null,
                purchase.getId(),
                purchase.getStatus(),
                purchase.getTotalAmount(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                KiosqueroRealtimeChangeKind.ORDER_CHANGED,
                message,
                purchase.getDate() == null ? null : purchase.getDate().toLocalDate(),
                purchase.getDate(),
                message
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
                productId,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                message
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
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                changeKind,
                reason,
                date,
                null,
                "Dashboard actualizado"
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
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                KiosqueroRealtimeChangeKind.DAILY_REPORT_CHANGED,
                reason,
                date,
                null,
                "Reporte diario actualizado"
        );
    }

    private static Integer dailyCapacityAvailable(Inventario inventory) {
        return inventory.getTipoManejo() == TipoManejoInventario.CUPO_DIARIO
                ? inventory.stockDisponible()
                : null;
    }

    private static boolean isAvailable(EstadoInventario status, int stockDisponible) {
        return status != EstadoInventario.DESACTIVADO
                && status != EstadoInventario.SIN_STOCK
                && stockDisponible > 0;
    }
}
