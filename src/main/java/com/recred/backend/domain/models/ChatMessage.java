package com.recred.backend.domain.models;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public record ChatMessage(
        UUID id,
        UUID sessionId,
        RolChatMensaje role,
        String content,
        Map<String, Object> metadata,
        LocalDateTime dateTime
) {
    public ChatMessage(UUID sessionId, RolChatMensaje role, String content,
                       Map<String, Object> metadata, LocalDateTime dateTime) {
        this(null, sessionId, role, content, metadata, dateTime);
    }
}
