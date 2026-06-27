package com.recred.backend.application;

import java.util.UUID;

public record CreateStudentFromParentRequest(
        String parentCognitoSubject,
        String username,
        String nombre,
        String apellido,
        String email,
        String dni,
        UUID gradoId,
        String urlFotoPerfil
) {
    public void validate() {
        requireNonBlank(nombre, "El nombre del alumno es obligatorio");
        requireNonBlank(apellido, "El apellido del alumno es obligatorio");
        requireNonBlank(email, "El email del alumno es obligatorio");

        if (username != null && username.contains("@")) {
            throw new IllegalArgumentException(
                    "El username no puede tener formato de email");
        }
    }

    private static void requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }
}
