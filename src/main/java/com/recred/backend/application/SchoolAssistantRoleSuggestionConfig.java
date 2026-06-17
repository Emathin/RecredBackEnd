package com.recred.backend.application;

import com.recred.backend.domain.models.SchoolAssistantActionType;
import com.recred.backend.domain.models.SchoolAssistantIntent;
import com.recred.backend.domain.models.SchoolAssistantSuggestion;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

record SchoolAssistantRoleSuggestionConfig(
        List<SchoolAssistantSuggestion> defaultSuggestions,
        EnumMap<SchoolAssistantIntent, List<SchoolAssistantSuggestion>> suggestionsByIntent,
        EnumSet<SchoolAssistantActionType> allowedActionTypes
) {

    List<SchoolAssistantSuggestion> suggestionsFor(SchoolAssistantIntent intent) {
        return suggestionsByIntent.getOrDefault(intent, defaultSuggestions);
    }

    boolean allows(SchoolAssistantActionType actionType) {
        return allowedActionTypes.contains(actionType);
    }

    static EnumMap<SchoolAssistantIntent, List<SchoolAssistantSuggestion>> suggestionsByIntent(
            Map<SchoolAssistantIntent, List<SchoolAssistantSuggestion>> suggestions
    ) {
        return new EnumMap<>(suggestions);
    }
}
