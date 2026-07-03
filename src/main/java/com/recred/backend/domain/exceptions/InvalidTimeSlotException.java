package com.recred.backend.domain.exceptions;

public class InvalidTimeSlotException extends RuntimeException {

    public InvalidTimeSlotException(String message) {
        super(message);
    }
}
