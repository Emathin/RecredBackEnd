package com.recred.backend.domain.exceptions;

import java.util.UUID;

public class ChatSessionNotFoundException extends RuntimeException {

    public ChatSessionNotFoundException(UUID sessionId) {
        super("No existe una sesion de chat abierta con id " + sessionId);
    }
}
