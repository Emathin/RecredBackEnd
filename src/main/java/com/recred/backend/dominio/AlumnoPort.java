package com.recred.backend.dominio;

import com.recred.backend.dominio.models.Alumno;
import java.util.Optional;
import java.util.UUID;

public interface AlumnoPort {
    Optional<Alumno> buscarPorId(UUID id);
    void guardar(Alumno alumno);
}