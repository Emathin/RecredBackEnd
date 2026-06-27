package com.recred.backend.domain.models;

import java.time.LocalDateTime;
import java.util.UUID;

public record MovimientoStock(
        UUID id,
        UUID inventarioId,
        TipoMovimientoStock tipo,
        Integer cantidad,
        Integer cantidadAnterior,
        Integer cantidadNueva,
        String motivo,
        UUID usuarioId,
        UUID compraId,
        LocalDateTime creadoEn
) {
    public boolean isRelease() {
        return tipo == TipoMovimientoStock.LIBERACION;
    }

    public int quantityOrZero() {
        return cantidad == null ? 0 : cantidad;
    }

    public boolean reachedZeroStock() {
        return cantidadNueva != null && cantidadNueva <= 0;
    }

    public boolean hasInventoryId() {
        return inventarioId != null;
    }
}
