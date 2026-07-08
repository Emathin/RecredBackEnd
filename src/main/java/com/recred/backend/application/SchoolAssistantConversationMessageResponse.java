package com.recred.backend.application;

import com.recred.backend.domain.models.ChatMessage;
import com.recred.backend.domain.models.RolChatMensaje;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public record SchoolAssistantConversationMessageResponse(
        UUID id,
        UUID sessionId,
        RolChatMensaje role,
        String content,
        Map<String, Object> metadata,
        LocalDateTime dateTime
) {
    public static SchoolAssistantConversationMessageResponse fromDomain(ChatMessage message) {
        return new SchoolAssistantConversationMessageResponse(
                message.id(),
                message.sessionId(),
                message.role(),
                message.content(),
                message.metadata(),
                message.dateTime()
        );
    }
}
