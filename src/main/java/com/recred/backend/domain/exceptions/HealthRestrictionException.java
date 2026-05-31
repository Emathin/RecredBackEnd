package com.recred.backend.domain.exceptions;

public class HealthRestrictionException extends RuntimeException {
    public HealthRestrictionException(String message) {
        super(message);
    }
}