package com.recred.backend.application;

import com.recred.backend.domain.models.SchoolAssistantContext;

public record SchoolAssistantResolvedContext(
        SchoolAssistantContext context,
        SchoolAssistantConversationOwner conversationOwner
) {
}
