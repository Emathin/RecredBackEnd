package com.recred.backend.control_parental.domain.ports.out;

import com.recred.backend.control_parental.domain.model.Alumno;
import java.util.Optional;
import java.util.UUID;

public interface AlumnoPort {
    Optional<Alumno> buscarPorId(UUID id);
    void guardar(Alumno alumno);
}