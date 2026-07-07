package com.recred.backend.application;

import java.util.UUID;

public record DetectStudentPreferencesRequest(
        UUID alumnoId,
        UUID buffetId
) {
    public void ensureRequiredFieldsPresent() {
        if (alumnoId == null) {
            throw new IllegalArgumentException("El alumno es obligatorio");
        }
        if (buffetId == null) {
            throw new IllegalArgumentException("El buffet es obligatorio");
        }
    }
}
