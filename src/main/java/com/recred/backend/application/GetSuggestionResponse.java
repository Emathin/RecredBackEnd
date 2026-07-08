package com.recred.backend.application;

import java.util.Map;
import java.util.UUID;

public record GetSuggestionResponse(
        UUID id,
        UUID alumnoId,
        UUID buffetId,
        UUID productoId,
        String titulo,
        String mensaje,
        Map<String, Object> metadata
) {
}
