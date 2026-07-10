package com.recred.backend.domain.exceptions;

public class GradeAlreadyExistsException extends RuntimeException {

    public GradeAlreadyExistsException(String anio, String division) {
        super("Ya existe un grado activo para el año " + anio
                + " y la división " + division);
    }
}
