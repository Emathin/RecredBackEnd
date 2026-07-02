package com.recred.backend.application;

import com.recred.backend.domain.models.TipoSugerenciaIA;

import java.util.UUID;

public record GetStudentPreferencesByUserRequest(
        UUID userId,
        boolean onlyLast,
        TipoSugerenciaIA type
) {

    public void validate() {
        if (userId == null) {
            throw new IllegalArgumentException("El usuario es obligatorio");
        }
    }
}
