package com.recred.backend.domain.exceptions;

public class RestriccionSaludException extends RuntimeException {
    public RestriccionSaludException(String message) {
        super(message);
    }
}