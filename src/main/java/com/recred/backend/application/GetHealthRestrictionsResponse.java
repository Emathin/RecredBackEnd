package com.recred.backend.application;

import java.util.UUID;

public record GetHealthRestrictionsResponse(
        UUID id,
        String descripcion,
        Boolean activo
) {}
