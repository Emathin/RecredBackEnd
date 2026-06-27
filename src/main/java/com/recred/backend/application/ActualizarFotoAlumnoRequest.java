package com.recred.backend.application;

import java.util.UUID;

public record ActualizarFotoAlumnoRequest(
        String parentCognitoSubject,
        UUID alumnoId,
        String urlFotoPerfil
) {}
