package com.recred.backend.application;

import com.recred.backend.domain.models.SugerenciaIA;
import com.recred.backend.domain.repositories.SuggestionAiRepository;

import java.util.UUID;

public class GetSuggestionByNotificationId {

    private final SuggestionAiRepository suggestionAiRepository;

    public GetSuggestionByNotificationId(SuggestionAiRepository suggestionAiRepository) {
        this.suggestionAiRepository = suggestionAiRepository;
    }

    public SugerenciaIA execute(UUID notificationId) {
        return suggestionAiRepository.getSuggestionByNotificationId(notificationId);
    }
}
