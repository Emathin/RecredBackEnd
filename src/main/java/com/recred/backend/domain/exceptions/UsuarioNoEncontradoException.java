package com.recred.backend.domain.exceptions;

import java.util.UUID;

public class UsuarioNoEncontradoException extends RuntimeException {

    public UsuarioNoEncontradoException(UUID usuarioId) {
        super("Usuario no encontrado: " + usuarioId);
    }
}
