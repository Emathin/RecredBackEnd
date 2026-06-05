package com.recred.backend.infrastructure.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.recred.backend.domain.models.ChatSession;
import com.recred.backend.domain.models.EstadoChatSesion;
import java.time.LocalDateTime;
import java.util.UUID;

public record SchoolAssistantSessionResponse(
        @JsonProperty("sesionId")
        UUID sessionId,
        @JsonProperty("estado")
        EstadoChatSesion status,
        @JsonProperty("fechaInicio")
        LocalDateTime startedAt,
        @JsonProperty("fechaUltimaActividad")
        LocalDateTime lastActivityAt
) {
    public static SchoolAssistantSessionResponse fromDomain(ChatSession session) {
        return new SchoolAssistantSessionResponse(
                session.id(),
                session.status(),
                session.startedAt(),
                session.lastActivityAt()
        );
    }
}
