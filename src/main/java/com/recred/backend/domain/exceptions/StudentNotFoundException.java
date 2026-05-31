package com.recred.backend.domain.exceptions;

import java.util.UUID;

public class StudentNotFoundException extends RuntimeException {

    public StudentNotFoundException(UUID alumnoId) {
        super("No existe el alumno con id " + alumnoId);
    }
}
