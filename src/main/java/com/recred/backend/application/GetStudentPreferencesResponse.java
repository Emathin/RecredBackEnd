package com.recred.backend.application;

import com.recred.backend.domain.models.TipoSugerenciaIA;

import java.util.List;
import java.util.UUID;

public record GetStudentPreferencesResponse(
        List<Preference> preferences
) {

    public record Preference(
            UUID suggestionId,
            UUID studentId,
            UUID studentUserId,
            String studentFirstName,
            String studentLastName,
            TipoSugerenciaIA type,
            String title,
            String message,
            String productId,
            String aiReason,
            boolean parentView
    ) {
    }
}
