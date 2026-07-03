package com.recred.backend.application;

import com.recred.backend.domain.models.ChatSession;
import com.recred.backend.domain.models.EstadoChatSesion;
import java.time.LocalDateTime;
import java.util.UUID;

public record SchoolAssistantConversationSessionResponse(
        UUID sessionId,
        EstadoChatSesion status,
        LocalDateTime startedAt,
        LocalDateTime lastActivityAt
) {
    public static SchoolAssistantConversationSessionResponse fromDomain(ChatSession session) {
        return new SchoolAssistantConversationSessionResponse(
                session.id(),
                session.status(),
                session.startedAt(),
                session.lastActivityAt()
        );
    }
}
