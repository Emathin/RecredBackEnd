package com.recred.backend.infrastructure.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.recred.backend.application.AskSchoolAssistantResponse.SuggestionResponse;

public record SchoolAssistantSuggestionResponse(
        @JsonProperty("label")
        String label,
        @JsonProperty("mensaje")
        String message,
        @JsonProperty("tipoAccion")
        String actionType
) {
    public static SchoolAssistantSuggestionResponse fromApplication(
            SuggestionResponse suggestion
    ) {
        return new SchoolAssistantSuggestionResponse(
                suggestion.label(),
                suggestion.message(),
                suggestion.actionType()
        );
    }
}
