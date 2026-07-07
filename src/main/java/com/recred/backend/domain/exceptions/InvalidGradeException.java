package com.recred.backend.domain.exceptions;

public class InvalidGradeException extends RuntimeException {

    public InvalidGradeException(String message) {
        super(message);
    }
}
