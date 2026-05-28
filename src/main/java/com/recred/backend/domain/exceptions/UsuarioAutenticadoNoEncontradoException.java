package com.recred.backend.domain.exceptions;

public class UsuarioAutenticadoNoEncontradoException extends RuntimeException {
    public UsuarioAutenticadoNoEncontradoException(String cognitoSubject) {
        super("No se encontro el usuario autenticado: " + cognitoSubject);
    }
}
