package com.recred.backend.domain.exceptions;

import java.util.UUID;

public class LevelNotFoundException extends RuntimeException {

    public LevelNotFoundException(UUID levelId) {
        super("No se encontro el nivel: " + levelId);
    }
}
