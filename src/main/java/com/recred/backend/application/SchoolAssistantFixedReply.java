package com.recred.backend.application;

import com.recred.backend.domain.models.SchoolAssistantAction;
import com.recred.backend.domain.models.SchoolAssistantIntent;
import com.recred.backend.domain.models.SchoolAssistantSuggestion;
import java.util.List;
import java.util.Map;

public record SchoolAssistantFixedReply(
        String answer,
        Map<String, Object> metadata,
        SchoolAssistantIntent intent,
        SchoolAssistantAction action,
        List<SchoolAssistantSuggestion> suggestions,
        Map<String, Object> data
) {
    public SchoolAssistantFixedReply(String answer, Map<String, Object> metadata) {
        this(
                answer,
                metadata,
                SchoolAssistantIntent.UNKNOWN,
                null,
                List.of(),
                Map.of()
        );
    }
}
