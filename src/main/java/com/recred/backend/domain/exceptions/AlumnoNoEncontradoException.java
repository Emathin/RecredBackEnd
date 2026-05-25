package com.recred.backend.domain.exceptions;

import java.util.UUID;

public class AlumnoNoEncontradoException extends RuntimeException {

    public AlumnoNoEncontradoException(UUID alumnoId) {
        super("No existe el alumno con id " + alumnoId);
    }
}
