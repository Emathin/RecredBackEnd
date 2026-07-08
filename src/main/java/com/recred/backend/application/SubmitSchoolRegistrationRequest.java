package com.recred.backend.application;


public record SubmitSchoolRegistrationRequest(
        String schoolName,
        String schoolEmail,
        String schoolPhone,
        String schoolCue,
        String directorFirstName,
        String directorLastName,
        String directorEmail,
        String directorPhone,
        String directorDni,
        String directorUsername
) {}
