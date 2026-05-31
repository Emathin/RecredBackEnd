package com.recred.backend.domain.exceptions;

public class TimeRestrictionException extends RuntimeException {
    public TimeRestrictionException(String message) {
        super(message);
    }
}