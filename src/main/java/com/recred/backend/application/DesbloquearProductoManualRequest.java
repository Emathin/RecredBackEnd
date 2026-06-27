package com.recred.backend.application;

import java.util.UUID;

public record DesbloquearProductoManualRequest(
        UUID alumnoId,
        UUID productoId
) {}
