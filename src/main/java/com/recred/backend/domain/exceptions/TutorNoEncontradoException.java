package com.recred.backend.domain.exceptions;

public class TutorNoEncontradoException extends RuntimeException {
    public TutorNoEncontradoException(String cognitoSubject) {
        super("No existe tutor asociado al usuario autenticado " + cognitoSubject);
    }
}
