package com.recred.backend.dominio.exceptions;

public class RestriccionSaludException extends RuntimeException {
    public RestriccionSaludException(String message) {
        super(message);
    }
}