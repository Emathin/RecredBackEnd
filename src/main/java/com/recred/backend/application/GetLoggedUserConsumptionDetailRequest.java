package com.recred.backend.application;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public record GetLoggedUserConsumptionDetailRequest(
        String cognitoSubject,
        UUID studentId,
        LocalDate from,
        LocalDate to
) {

    public Optional<UUID> requestedStudentId() {
        return Optional.ofNullable(studentId);
    }

    public Optional<LocalDate> requestedFrom() {
        return Optional.ofNullable(from);
    }

    public Optional<LocalDate> requestedTo() {
        return Optional.ofNullable(to);
    }
}
