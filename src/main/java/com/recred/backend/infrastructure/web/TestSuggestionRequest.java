package com.recred.backend.infrastructure.web;

import java.util.UUID;

public record TestSuggestionRequest(
        String testCognitoId,
        UUID sugerenciaId
) {
}
