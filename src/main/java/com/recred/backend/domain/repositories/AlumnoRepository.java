package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.Alumno;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AlumnoRepository {

    Optional<Alumno> findById(UUID id);

    List<Alumno> findAll();

    Alumno save(Alumno alumno);

    Optional<UUID> findStudentIdByUserId(UUID userId);

    boolean existsById(UUID alumnoId);

    UUID findUserID(UUID alumnoId);

}
