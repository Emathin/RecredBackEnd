package com.recred.backend.application;

import com.recred.backend.domain.models.EstadoInventario;
import com.recred.backend.domain.models.TipoManejoInventario;

import java.util.UUID;

public record AdjustInventoryStockCommand(
        UUID buffetId,
        UUID productId,
        Integer stockActual,
        Integer stockMinimo,
        EstadoInventario estadoInventario,
        TipoManejoInventario tipoManejoInventario,
        Boolean disponible,
        Integer cupoMaximoDiario,
        String motivo,
        UUID usuarioId
) {
}
