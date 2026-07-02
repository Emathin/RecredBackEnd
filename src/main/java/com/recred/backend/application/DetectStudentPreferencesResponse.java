package com.recred.backend.application;

import com.recred.backend.domain.repositories.PreferenceAIRepository.PreferenciaDetectada;

import java.util.UUID;

public record DetectStudentPreferencesResponse(
        String titulo,
        String mensaje,
        UUID productoId,
        String razonIA
) {
    public static DetectStudentPreferencesResponse fromDetectedPreference(
            PreferenciaDetectada preference,
            UUID productoId
    ) {
        return new DetectStudentPreferencesResponse(
                preference.titulo(),
                preference.mensaje(),
                productoId,
                preference.razonIA()
        );
    }
}
