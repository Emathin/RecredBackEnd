package com.recred.backend.domain.models;

import java.util.List;

public record SchoolAssistantPrompt(
        String question,
        SchoolAssistantContext context,
        List<ChatMessage> recentMessages
) {
}
