package com.recred.backend.domain.models;

public record DailyMovementReport(
        TipoMovimientoStock movementType,
        int quantity
) {
}
