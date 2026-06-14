package com.recred.backend.domain.events;

public enum KiosqueroRealtimeChangeKind {
    STOCK_ONLY,
    CONFIG_CHANGED,
    SALE,
    RESERVATION,
    RESERVATION_RELEASED,
    RESERVATION_CONSUMED,
    LOW_STOCK,
    SOLD_OUT,
    DAILY_REPORT_CHANGED,
    ORDER_CHANGED
}
