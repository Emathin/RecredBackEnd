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
}
