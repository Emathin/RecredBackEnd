package com.recred.backend.domain.exceptions;

public class ActiveCreditExistsException extends RuntimeException {
    public ActiveCreditExistsException(String message) {
        super(message);
    }
}
