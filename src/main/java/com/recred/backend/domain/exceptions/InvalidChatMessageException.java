package com.recred.backend.domain.exceptions;

public class InvalidChatMessageException extends RuntimeException {

    public InvalidChatMessageException() {
        super("El mensaje del asistente no puede estar vacio.");
    }
}
