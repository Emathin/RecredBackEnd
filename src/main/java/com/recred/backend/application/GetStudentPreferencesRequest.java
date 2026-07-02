package com.recred.backend.application;

import com.recred.backend.domain.models.TipoSugerenciaIA;

import java.util.UUID;

public record GetStudentPreferencesRequest(
        UUID studentId,
        boolean onlyLast,
        TipoSugerenciaIA type
) {

    public void validate() {
        if (studentId == null) {
            throw new IllegalArgumentException("El alumno es obligatorio");
        }
    }
}
