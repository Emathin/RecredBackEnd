package com.recred.backend.application;

import com.recred.backend.domain.models.TipoNotificacion;
import java.time.LocalDateTime;
import java.util.UUID;

public record FindPurchaseSuggestionResponse(
        UUID id,
        TipoNotificacion tipo,
        String titulo,
        String mensaje,
        Boolean leida,
        LocalDateTime creadoEn,
        LocalDateTime leidaEn
) {
}
