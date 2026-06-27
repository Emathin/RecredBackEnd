package com.recred.backend.application;

import java.util.UUID;

public record DeleteFavoriteCartRequest(
        String cognitoSub,
        UUID cartId
) {}
