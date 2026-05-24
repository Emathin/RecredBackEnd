package com.recred.backend.dominio.repositories;

import com.recred.backend.dominio.models.Alumno;

import java.util.Optional;
import java.util.UUID;

public interface AlumnoRepository {

    Optional<Alumno> findById(UUID id);

    void save(Alumno alumno);
}