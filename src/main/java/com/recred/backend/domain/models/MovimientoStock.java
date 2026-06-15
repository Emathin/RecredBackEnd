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
}
