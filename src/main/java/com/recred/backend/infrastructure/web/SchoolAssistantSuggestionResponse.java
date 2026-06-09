package com.recred.backend.infrastructure.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.recred.backend.domain.models.SchoolAssistantActionType;
import com.recred.backend.domain.models.SchoolAssistantSuggestion;

public record SchoolAssistantSuggestionResponse(
        @JsonProperty("label")
        String label,
        @JsonProperty("mensaje")
        String message,
        @JsonProperty("tipoAccion")
        SchoolAssistantActionType actionType
) {
    public static SchoolAssistantSuggestionResponse fromDomain(
            SchoolAssistantSuggestion suggestion
    ) {
        return new SchoolAssistantSuggestionResponse(
                suggestion.label(),
                suggestion.message(),
                suggestion.actionType()
        );
    }
}
