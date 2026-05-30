package com.recred.backend.domain.exceptions;

public class ImageValidationException extends RuntimeException {
    public ImageValidationException(String message) {
        super(message);
    }
}
