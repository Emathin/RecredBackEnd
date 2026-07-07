package com.recred.backend.domain.exceptions;

import java.util.UUID;

public class GradeNotFoundException extends RuntimeException {

    public GradeNotFoundException(UUID gradeId) {
        super("No se encontro el grado: " + gradeId);
    }
}
