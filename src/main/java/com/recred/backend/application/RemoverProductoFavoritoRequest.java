package com.recred.backend.application;

import java.util.UUID;

public record RemoverProductoFavoritoRequest(
        UUID alumnoId,
        UUID productoId
) {}
