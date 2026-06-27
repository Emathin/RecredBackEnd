package com.recred.backend.application;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record SaveCarritoFavoritoResponse(
        UUID id,
        String nombre,
        UUID tutorId,
        UUID alumnoId,
        String alumnoNombre,
        String alumnoApellido,
        List<CarritoFavoritoItemResponse> items
) {
    public record CarritoFavoritoItemResponse(
            UUID productId,
            String productoNombre,
            BigDecimal productoPrecio,
            Integer quantity
    ) {}
}
