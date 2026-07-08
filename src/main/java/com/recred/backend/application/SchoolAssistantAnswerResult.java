package com.recred.backend.application;

import com.recred.backend.domain.models.SchoolAssistantAnswer;
import com.recred.backend.domain.models.SchoolAssistantContext;
import java.time.LocalDateTime;
import java.util.Map;

public record SchoolAssistantAnswerResult(
        SchoolAssistantAnswer answer,
        Map<String, Object> additionalMetadata
) {
    public static SchoolAssistantAnswerResult fromFixedReply(
            SchoolAssistantConversation conversation,
            SchoolAssistantFixedReply reply,
            SchoolAssistantContext context,
            LocalDateTime now
    ) {
        return new SchoolAssistantAnswerResult(
                new SchoolAssistantAnswer(
                        conversation.sessionId(),
                        reply.answer(),
                        context.capabilities(),
                        now,
                        false,
                        null,
                        context.role(),
                        reply.intent(),
                        reply.action(),
                        reply.suggestions(),
                        reply.data()
                ),
                reply.metadata()
        );
    }
}
