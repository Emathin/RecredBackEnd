package com.recred.backend.application;

import java.util.UUID;

public record GenerateComboSuggestionRequest(
        UUID productId,
        String cognitoSubject
) {
}
