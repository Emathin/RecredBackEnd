package com.recred.backend.application;

import java.util.UUID;

public record RegisterSellerResponse(
        UUID kiosqueroId,
        UUID usuarioId
) {
}
