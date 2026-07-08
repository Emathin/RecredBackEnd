package com.recred.backend.domain.exceptions;

import java.util.UUID;

public class BuffetAlreadyExistsForSchoolException extends RuntimeException {

    public BuffetAlreadyExistsForSchoolException(UUID schoolId) {
        super("Ya existe un buffet para el colegio " + schoolId);
    }
}
