package com.recred.backend.domain.models;

import java.util.UUID;

public record DailyInventorySnapshot(
        UUID productId,
        String productName,
        Integer stockActual,
        Integer stockReservado,
        Integer stockDisponible,
        Integer stockMinimo,
        Integer cupoMaximoDiario,
        Integer cupoDisponibleDia,
        EstadoInventario estadoInventario,
        TipoManejoInventario tipoManejoInventario
) {
}
