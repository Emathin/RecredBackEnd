package com.recred.backend.domain.exceptions;

public class InventoryOperationException extends RuntimeException {
    public InventoryOperationException(String message) {
        super(message);
    }
}
