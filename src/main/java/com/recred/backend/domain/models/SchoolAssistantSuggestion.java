package com.recred.backend.domain.models;

public record SchoolAssistantSuggestion(
        String label,
        String message,
        SchoolAssistantActionType actionType
) {
}
