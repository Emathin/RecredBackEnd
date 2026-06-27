package com.recred.backend.application;

import java.util.UUID;

public record AddFavoriteProductRequest(
        UUID alumnoId,
        UUID productoId
) {}
