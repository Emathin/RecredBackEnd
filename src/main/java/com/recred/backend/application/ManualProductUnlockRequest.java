package com.recred.backend.application;

import java.util.UUID;

public record ManualProductUnlockRequest(
        UUID alumnoId,
        UUID productoId
) {}
