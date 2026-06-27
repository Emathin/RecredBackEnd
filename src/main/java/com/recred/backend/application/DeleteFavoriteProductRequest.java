package com.recred.backend.application;

import java.util.UUID;

public record DeleteFavoriteProductRequest(
        UUID alumnoId,
        UUID productoId
) {}
