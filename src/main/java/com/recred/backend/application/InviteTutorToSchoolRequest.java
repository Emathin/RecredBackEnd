package com.recred.backend.application;

public record InviteTutorToSchoolRequest(
        String directorCognitoSubject,
        String email,
        String firstName,
        String lastName,
        String phone
) {
}
