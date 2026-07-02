package com.recred.backend.application;

import com.recred.backend.domain.models.TipoSugerenciaIA;

import java.util.UUID;

public record GetStudentPreferencesRequest(
        UUID studentId,
        UUID userId,
        boolean onlyLast,
        TipoSugerenciaIA type
) {

    public GetStudentPreferencesRequest(
            UUID studentId,
            boolean onlyLast,
            TipoSugerenciaIA type
    ) {
        this(studentId, null, onlyLast, type);
    }

    public static GetStudentPreferencesRequest byStudent(
            UUID studentId,
            boolean onlyLast,
            TipoSugerenciaIA type
    ) {
        return new GetStudentPreferencesRequest(studentId, null, onlyLast, type);
    }

    public static GetStudentPreferencesRequest byUser(
            UUID userId,
            boolean onlyLast,
            TipoSugerenciaIA type
    ) {
        return new GetStudentPreferencesRequest(null, userId, onlyLast, type);
    }

    public void validate() {
        if (studentId == null && userId == null) {
            throw new IllegalArgumentException("El alumno o usuario es obligatorio");
        }
    }

    public boolean isByUser() {
        return userId != null;
    }
}
