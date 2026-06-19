package com.recred.backend.domain.events;

import com.recred.backend.domain.models.EstadoInventario;
import com.recred.backend.domain.models.TipoManejoInventario;
import com.recred.backend.domain.models.TipoMovimientoStock;
import java.util.UUID;

public record InventoryRealtimePayload(
        UUID productId,
        UUID inventoryId,
        TipoMovimientoStock movementType,
        Integer stockActual,
        Integer stockReservado,
        Integer stockDisponible,
        Integer stockMinimo,
        Integer cupoMaximoDiario,
        Integer cupoDisponibleDia,
        Boolean disponible,
        EstadoInventario estadoInventario,
        TipoManejoInventario tipoManejoInventario
) implements KiosqueroRealtimePayload {
}
