package com.recred.backend.infrastructure.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.recred.backend.application.SchoolAssistantConversationMessageResponse;
import com.recred.backend.domain.models.RolChatMensaje;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public record SchoolAssistantMessageResponse(
        @JsonProperty("id")
        UUID id,
        @JsonProperty("sesionId")
        UUID sessionId,
        @JsonProperty("rol")
        RolChatMensaje role,
        @JsonProperty("contenido")
        String content,
        @JsonProperty("metadata")
        Map<String, Object> metadata,
        @JsonProperty("fechaHora")
        LocalDateTime dateTime
) {
    public static SchoolAssistantMessageResponse fromApplication(
            SchoolAssistantConversationMessageResponse message
    ) {
        return new SchoolAssistantMessageResponse(
                message.id(),
                message.sessionId(),
                message.role(),
                message.content(),
                message.metadata(),
                message.dateTime()
        );
    }
}
