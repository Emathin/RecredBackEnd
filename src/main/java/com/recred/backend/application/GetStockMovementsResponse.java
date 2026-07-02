package com.recred.backend.application;

import com.recred.backend.domain.models.MovimientoStock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record GetStockMovementsResponse(List<StockMovementResponse> movements) {

    public static GetStockMovementsResponse fromDomain(List<MovimientoStock> movements) {
        return new GetStockMovementsResponse(movements.stream()
                .map(StockMovementResponse::fromDomain)
                .toList());
    }

    public record StockMovementResponse(
            UUID id,
            UUID inventarioId,
            String tipo,
            Integer cantidad,
            Integer cantidadAnterior,
            Integer cantidadNueva,
            String motivo,
            UUID usuarioId,
            UUID compraId,
            LocalDateTime creadoEn
    ) {
        private static StockMovementResponse fromDomain(MovimientoStock movement) {
            return new StockMovementResponse(
                    movement.id(),
                    movement.inventarioId(),
                    movement.tipo() == null ? null : movement.tipo().name(),
                    movement.cantidad(),
                    movement.cantidadAnterior(),
                    movement.cantidadNueva(),
                    movement.motivo(),
                    movement.usuarioId(),
                    movement.compraId(),
                    movement.creadoEn()
            );
        }
    }
}
