package com.recred.backend.domain.exceptions;

public class ExceededBudgetException extends RuntimeException {
    public ExceededBudgetException(String message) {
        super(message);
    }
}
