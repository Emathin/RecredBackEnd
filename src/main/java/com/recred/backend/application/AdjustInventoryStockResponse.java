package com.recred.backend.application;

import com.recred.backend.domain.models.EstadoInventario;
import com.recred.backend.domain.models.Inventario;
import java.util.UUID;

public record AdjustInventoryStockResponse(
        UUID id,
        UUID buffetId,
        UUID productoId,
        Integer stockActual,
        Integer stockReservado,
        Integer stockMinimo,
        Integer stockDisponible,
        String estadoInventario,
        String tipoManejoInventario,
        Integer cupoMaximoDiario,
        String urlImagen
) {
    public static AdjustInventoryStockResponse fromDomain(Inventario inventory) {
        EstadoInventario status = inventory.estadoActual();
        return new AdjustInventoryStockResponse(
                inventory.getId(),
                inventory.getBuffetId(),
                inventory.getProductoId(),
                inventory.getStockActual(),
                inventory.getStockReservado(),
                inventory.stockMinimoEfectivo(),
                inventory.stockDisponible(),
                status == null ? null : status.name(),
                inventory.getTipoManejo() == null ? null : inventory.getTipoManejo().name(),
                inventory.getCupoMaximoDiario(),
                inventory.getUrlImagen()
        );
    }
}
