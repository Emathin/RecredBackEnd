package com.recred.backend.application;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public record GetStudentWalletSummaryRequest(
        String cognitoSubject,
        UUID studentId,
        Optional<LocalDate> from,
        Optional<LocalDate> to
) {

    public GetStudentWalletSummaryRequest {
        from = from == null ? Optional.empty() : from;
        to = to == null ? Optional.empty() : to;
    }

    public void validate() {
        if (cognitoSubject == null || cognitoSubject.isBlank()) {
            throw new IllegalArgumentException("El usuario autenticado es obligatorio");
        }
        if (studentId == null) {
            throw new IllegalArgumentException("El alumno es obligatorio");
        }
    }
}
