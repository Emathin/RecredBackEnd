package com.recred.backend.domain.models;

import java.util.UUID;

public record ConsumptionRequester(
        UUID userId,
        RolUsuario role,
        UUID studentId
) {
}
