package com.recred.backend.application;

import java.util.UUID;

public record GetFavoriteCartsByTutorRequest(
        String cognitoSub,
        UUID alumnoId,
        UUID productoId
) {}
