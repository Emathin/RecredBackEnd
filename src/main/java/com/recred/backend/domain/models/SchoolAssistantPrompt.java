package com.recred.backend.domain.models;

import java.util.List;

public record SchoolAssistantPrompt(
        String question,
        SchoolAssistantQuery query,
        SchoolAssistantContext context,
        List<ChatMessage> recentMessages
) {
}
