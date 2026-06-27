package com.recred.backend.application;

import java.util.UUID;

public record UpdateStudentPhotoRequest(
        String parentCognitoSubject,
        UUID alumnoId,
        String urlFotoPerfil
) {}
