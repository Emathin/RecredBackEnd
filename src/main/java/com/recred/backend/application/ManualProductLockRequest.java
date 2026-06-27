package com.recred.backend.application;

import java.util.UUID;

public record ManualProductLockRequest(
        UUID alumnoId,
        UUID productoId
) {}
