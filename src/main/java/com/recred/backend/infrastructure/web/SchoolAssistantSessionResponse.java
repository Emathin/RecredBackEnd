package com.recred.backend.infrastructure.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.recred.backend.application.SchoolAssistantConversationSessionResponse;
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
    public static SchoolAssistantSessionResponse fromApplication(
            SchoolAssistantConversationSessionResponse session
    ) {
        return new SchoolAssistantSessionResponse(
                session.sessionId(),
                session.status(),
                session.startedAt(),
                session.lastActivityAt()
        );
    }
}
