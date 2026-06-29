package com.recred.backend.domain.models;

import java.util.UUID;

public record ConsumptionRequester(
        UUID userId,
        RolUsuario role,
        UUID studentId
) {

    public boolean isStudentFor(UUID requestedStudentId) {
        return role == RolUsuario.ALUMNO
                && studentId != null
                && studentId.equals(requestedStudentId);
    }

    public boolean isStudent() {
        return role == RolUsuario.ALUMNO;
    }

    public boolean isTutor() {
        return role == RolUsuario.PADRE;
    }

    public boolean hasStudentId() {
        return studentId != null;
    }
}
