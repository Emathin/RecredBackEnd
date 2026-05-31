package com.recred.backend.domain.exceptions;

public class InvalidChatMessageException extends RuntimeException {

    public InvalidChatMessageException() {
        super("El message del asistente no puede estar vacio.");
    }
}
