package com.recred.backend.domain.exceptions;

import java.util.UUID;

public class BuffetNotFoundException extends RuntimeException {

    public BuffetNotFoundException(UUID buffetId) {
        super("No se encontro el buffet: " + buffetId);
    }
}
