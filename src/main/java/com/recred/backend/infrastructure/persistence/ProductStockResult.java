package com.recred.backend.infrastructure.persistence;

import com.recred.backend.domain.models.EstadoInventario;
import com.recred.backend.domain.models.TipoManejoInventario;
import com.recred.backend.infrastructure.entities.ProductoEntity;

public record ProductStockResult(
        ProductoEntity product,
        Number stockActual,
        Number stockReservado,
        Number stockMinimo,
        EstadoInventario estado,
        TipoManejoInventario tipoManejo,
        Number cupoMaximoDiario,
        Number stockDisponible
) {
}
