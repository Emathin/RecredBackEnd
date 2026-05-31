package com.recred.backend.domain.exceptions;

public class AuthenticatedUserNotFoundException extends RuntimeException {
    public AuthenticatedUserNotFoundException(String cognitoSubject) {
        super("No se encontro el usuario autenticado: " + cognitoSubject);
    }
}
