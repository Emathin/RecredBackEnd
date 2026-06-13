package com.recred.backend.domain.exceptions;

import java.util.UUID;

public class SchoolNotFoundException extends RuntimeException {

    public SchoolNotFoundException(UUID schoolId) {
        super("School not found: " + schoolId);
    }
}
