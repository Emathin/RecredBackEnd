package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.Alumno;

import java.util.Optional;
import java.util.UUID;

public interface AlumnoRepository {

    Optional<Alumno> findById(UUID id);

    void save(Alumno alumno);

    boolean existsById(UUID alumnoId);
}
