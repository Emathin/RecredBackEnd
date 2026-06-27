package com.recred.backend.application;

import java.util.UUID;

public record BloquearProductoManualRequest(
        UUID alumnoId,
        UUID productoId
) {}
