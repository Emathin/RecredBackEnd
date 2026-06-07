package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.Tutor;

import java.util.Optional;
import java.util.UUID;

public interface TutorRepository {
    Optional<Tutor> findById(UUID id);
    Optional<Tutor> findByUsuarioId(UUID usuarioId);
    Optional<Tutor> findByUsuarioIdCognito(String cognitoSubject);
}
