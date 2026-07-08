package com.recred.backend.application;

public record AcceptTutorInvitationRequest(
        String token,
        String cognitoSubject,
        String authenticatedEmail,
        String firstName,
        String lastName,
        String phone,
        String documentNumber
) {
}
