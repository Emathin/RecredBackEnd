package com.recred.backend.application;

import java.util.UUID;

public record GetCarritosFavoritosByTutorRequest(
        String cognitoSub,
        UUID alumnoId,
        UUID productoId
) {}
