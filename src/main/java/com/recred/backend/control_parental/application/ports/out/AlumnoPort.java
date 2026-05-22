package com.recred.backend.control_parental.application.ports.out;

import com.recred.backend.usuarios.infrastructure.entities.AlumnoEntity;

import java.util.Optional;
import java.util.UUID;

public interface AlumnoPort {
    Optional<AlumnoEntity> buscarPorId(UUID id);
    AlumnoEntity guardar(AlumnoEntity alumno);
}