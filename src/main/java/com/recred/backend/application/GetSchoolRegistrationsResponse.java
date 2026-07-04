package com.recred.backend.application;

import com.recred.backend.domain.models.Nivel;
import com.recred.backend.domain.models.SchoolRegistrationRequest;

import java.util.UUID;

public record GetSchoolRegistrationsResponse(
        UUID id,
        String schoolName,
        String schoolEmail,
        String schoolPhone,
        String schoolCue,
        Nivel schoolLevel,
        String directorFirstName,
        String directorLastName,
        String directorEmail,
        String directorPhone,
        String directorDni,
        String directorUsername,
        String status,
        String createdAt
) {
    public static GetSchoolRegistrationsResponse fromDomain(SchoolRegistrationRequest req) {
        return new GetSchoolRegistrationsResponse(
                req.getId(),
                req.getSchoolName(),
                req.getSchoolEmail(),
                req.getSchoolPhone(),
                req.getSchoolCue(),
                req.getSchoolLevel(),
                req.getDirectorFirstName(),
                req.getDirectorLastName(),
                req.getDirectorEmail(),
                req.getDirectorPhone(),
                req.getDirectorDni(),
                req.getDirectorUsername(),
                req.getStatus() != null ? req.getStatus().name() : null,
                req.getCreatedAt() != null ? req.getCreatedAt().toString() : null
        );
    }
}
