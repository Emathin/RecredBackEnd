package com.recred.backend.domain.exceptions;

import java.util.UUID;

public class GradeAlreadyExistsException extends RuntimeException {

    public GradeAlreadyExistsException(UUID colegioId, String anio, String division) {
        super("Ya existe un grado activo " + anio + " " + division
                + " para el colegio " + colegioId);
    }
}
