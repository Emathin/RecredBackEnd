package com.recred.backend.domain.events;

public sealed interface KiosqueroRealtimePayload
        permits EmptyRealtimePayload,
                InventoryRealtimePayload,
                ProductAlertRealtimePayload,
                PurchaseRealtimePayload {
}
