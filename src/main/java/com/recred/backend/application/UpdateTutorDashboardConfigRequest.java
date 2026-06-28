package com.recred.backend.application;

public record UpdateTutorDashboardConfigRequest(
        String cognitoSubject,
        String dashboardConfig
) {
    public void validate() {
        if (cognitoSubject == null || cognitoSubject.trim().isEmpty()) {
            throw new IllegalArgumentException("El usuario es obligatorio");
        }
        if (dashboardConfig == null) {
            throw new IllegalArgumentException("La configuración del dashboard es obligatoria");
        }
    }
}
