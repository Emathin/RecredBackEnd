package com.recred.backend.application;

import com.recred.backend.domain.models.TipoSugerenciaIA;

public record GetSuggestionByTypeRequest(
        String cognitoSubject,
        TipoSugerenciaIA tipo
) {
}
