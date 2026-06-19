package com.recred.backend.infrastructure.web;

import java.util.List;
import java.util.UUID;

public record SaveCarritoFavoritoRequest(
        UUID id,
        String nombre,
        UUID alumnoId,
        List<ItemRequest> items
) {
    public record ItemRequest(
            UUID productId,
            Integer quantity
    ) {}
}
