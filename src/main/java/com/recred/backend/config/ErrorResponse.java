package com.recred.backend.config;

import java.time.LocalDateTime;

public record ErrorResponse(
        String error,
        String mensaje,
        LocalDateTime timestamp,
        int status,
        String requestId
) {}
