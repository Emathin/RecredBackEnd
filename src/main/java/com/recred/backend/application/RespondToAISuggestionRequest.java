package com.recred.backend.application;

import java.util.UUID;

public record RespondToAISuggestionRequest(
        UUID suggestionId,
        boolean accepted
) {

    public void validate() {
        if (suggestionId == null) {
            throw new IllegalArgumentException("La sugerencia es obligatoria");
        }
    }
}
