package com.recred.backend.application;

import com.recred.backend.domain.models.Nivel;

public record SubmitSchoolRegistrationRequest(
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
        String directorUsername
) {}
