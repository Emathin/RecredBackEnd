package com.recred.backend.application;

import java.util.UUID;

public record AgregarProductoFavoritoRequest(
        UUID alumnoId,
        UUID productoId
) {}
