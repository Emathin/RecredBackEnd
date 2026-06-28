package com.recred.backend.application;

public record GetTutorGlobalDashboardRequest(
        String cognitoSubject
) {
    public void validate() {
        if (cognitoSubject == null || cognitoSubject.trim().isEmpty()) {
            throw new IllegalArgumentException("El usuario autenticado es obligatorio");
        }
    }
}
