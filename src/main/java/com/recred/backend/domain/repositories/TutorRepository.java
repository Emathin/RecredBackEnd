package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.Tutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TutorRepository {
    Optional<Tutor> findById(UUID id);

    Optional<Tutor> findByUsuarioId(UUID usuarioId);

    Optional<Tutor> findByUsuarioIdCognito(String cognitoSubject);

    Tutor save(Tutor tutor);

    List<Tutor> findAll();

    List<Tutor> findByAlumnoId(UUID alumnoId);
}
