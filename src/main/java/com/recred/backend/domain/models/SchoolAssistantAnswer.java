package com.recred.backend.domain.models;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record SchoolAssistantAnswer(
        UUID sessionId,
        String answer,
        List<SchoolAssistantCapability> capabilities,
        LocalDateTime dateTime,
        boolean generatedByAi,
        String model,
        SchoolAssistantRole role,
        SchoolAssistantIntent intent,
        SchoolAssistantAction action,
        List<SchoolAssistantSuggestion> suggestions,
        Map<String, Object> data
) {
    public SchoolAssistantAnswer(
            UUID sessionId,
            String answer,
            List<SchoolAssistantCapability> capabilities,
            LocalDateTime dateTime,
            boolean generatedByAi,
            String model
    ) {
        this(
                sessionId,
                answer,
                capabilities,
                dateTime,
                generatedByAi,
                model,
                null,
                SchoolAssistantIntent.UNKNOWN,
                null,
                List.of(),
                Map.of()
        );
    }
}
