package com.recred.backend.application;

import com.recred.backend.domain.models.ChatMessage;
import com.recred.backend.domain.models.ChatSession;
import java.util.List;
import java.util.UUID;

public record SchoolAssistantConversation(
        ChatSession session,
        List<ChatMessage> recentMessages
) {

    public UUID sessionId() {
        return session.id();
    }
}
