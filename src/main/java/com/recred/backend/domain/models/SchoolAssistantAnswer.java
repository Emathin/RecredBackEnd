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

    public List<String> capabilityNames() {
        return capabilitiesOrEmpty().stream()
                .map(Enum::name)
                .toList();
    }

    public List<SchoolAssistantCapability> capabilitiesOrEmpty() {
        return capabilities == null ? List.of() : capabilities;
    }

    public String roleName() {
        return role == null ? null : role.name();
    }

    public String intentName() {
        return intent == null ? null : intent.name();
    }

    public List<SchoolAssistantSuggestion> suggestionsOrEmpty() {
        return suggestions == null ? List.of() : suggestions;
    }

    public Map<String, Object> dataOrEmpty() {
        return data == null ? Map.of() : data;
    }
}
