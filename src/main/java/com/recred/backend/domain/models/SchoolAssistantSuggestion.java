package com.recred.backend.domain.models;

public record SchoolAssistantSuggestion(
        String label,
        String message,
        SchoolAssistantActionType actionType
) {

    public String actionTypeName() {
        return actionType == null ? null : actionType.name();
    }
}
