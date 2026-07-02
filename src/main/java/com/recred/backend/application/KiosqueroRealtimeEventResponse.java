package com.recred.backend.application;

import com.recred.backend.domain.events.InventoryRealtimePayload;
import com.recred.backend.domain.events.KiosqueroRealtimeEvent;
import com.recred.backend.domain.events.KiosqueroRealtimePayload;
import com.recred.backend.domain.events.ProductAlertRealtimePayload;
import com.recred.backend.domain.events.PurchaseRealtimePayload;
import java.math.BigDecimal;
import java.time.LocalDate;
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
        LocalDate date,
        LocalDateTime occurredAt,
        String message
) {
    public static KiosqueroRealtimeEventResponse fromDomain(KiosqueroRealtimeEvent event) {
        PayloadResponseFields payload = PayloadResponseFields.fromDomain(event.payload());
        return new KiosqueroRealtimeEventResponse(
                event.buffetId(),
                event.type().name(),
                payload.productId(),
                payload.inventoryId(),
                payload.purchaseId(),
                payload.purchaseStatus(),
                payload.purchaseTotal(),
                payload.movementType(),
                payload.stockActual(),
                payload.stockReservado(),
                payload.stockDisponible(),
                payload.stockMinimo(),
                payload.cupoMaximoDiario(),
                payload.cupoDisponibleDia(),
                payload.disponible(),
                payload.estadoInventario(),
                payload.tipoManejoInventario(),
                enumName(event.changeKind()),
                event.reason(),
                event.date(),
                event.occurredAt(),
                event.message()
        );
    }

    private static String enumName(Enum<?> value) {
        return value == null ? null : value.name();
    }

    private record PayloadResponseFields(
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
            String tipoManejoInventario
    ) {
        private static PayloadResponseFields fromDomain(KiosqueroRealtimePayload payload) {
            PayloadResponseFields fields = empty();
            fields = payload instanceof InventoryRealtimePayload inventory
                    ? fromInventory(inventory)
                    : fields;
            fields = payload instanceof PurchaseRealtimePayload purchase
                    ? fromPurchase(purchase)
                    : fields;
            return payload instanceof ProductAlertRealtimePayload productAlert
                    ? fromProductAlert(productAlert)
                    : fields;
        }

        private static PayloadResponseFields fromInventory(InventoryRealtimePayload payload) {
            return new PayloadResponseFields(
                    payload.productId(),
                    payload.inventoryId(),
                    null,
                    null,
                    null,
                    enumName(payload.movementType()),
                    payload.stockActual(),
                    payload.stockReservado(),
                    payload.stockDisponible(),
                    payload.stockMinimo(),
                    payload.cupoMaximoDiario(),
                    payload.cupoDisponibleDia(),
                    payload.disponible(),
                    enumName(payload.estadoInventario()),
                    enumName(payload.tipoManejoInventario())
            );
        }

        private static PayloadResponseFields fromPurchase(PurchaseRealtimePayload payload) {
            return new PayloadResponseFields(
                    null,
                    null,
                    payload.purchaseId(),
                    enumName(payload.purchaseStatus()),
                    payload.purchaseTotal(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );
        }

        private static PayloadResponseFields fromProductAlert(
                ProductAlertRealtimePayload payload
        ) {
            return new PayloadResponseFields(
                    payload.productId(),
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
                    null
            );
        }

        private static PayloadResponseFields empty() {
            return new PayloadResponseFields(
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
                    null
            );
        }
    }
}
