package com.recred.backend.application;

public record ImportTutorInvitationsFromCsvRequest(
        String directorCognitoSubject,
        String csvContent
) {
}
