package com.recred.backend.application;

import java.util.Map;
import java.util.UUID;

public record SaveFavoriteCartRequest(
        String cognitoSub,
        UUID cartId,
        String nombre,
        UUID alumnoId,
        Map<UUID, Integer> productQuantities
) {}
