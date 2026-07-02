package com.recred.backend.application;

import com.recred.backend.domain.models.SugerenciaIA;
import com.recred.backend.domain.repositories.SuggestionAiRepository;

public class GetSuggestionByNotificationId {

    private final SuggestionAiRepository suggestionAiRepository;

    public GetSuggestionByNotificationId(SuggestionAiRepository suggestionAiRepository) {
        this.suggestionAiRepository = suggestionAiRepository;
    }

    public GetSuggestionResponse execute(GetSuggestionByNotificationIdRequest request) {
        SugerenciaIA suggestion = suggestionAiRepository.getSuggestionByNotificationId(
                request.notificationId());
        if (suggestion == null) {
            return null;
        }
        return mapToResponse(suggestion);
    }

    private GetSuggestionResponse mapToResponse(SugerenciaIA suggestion) {
        return new GetSuggestionResponse(
                suggestion.getId(),
                suggestion.getAlumnoId(),
                suggestion.getBuffetId(),
                suggestion.getProductoId(),
                suggestion.getTitulo(),
                suggestion.getMensaje(),
                suggestion.getMetadata()
        );
    }
}
