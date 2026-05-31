package com.recred.backend.domain.exceptions;

import java.util.UUID;

public class NotAuthorizedStudentConsumptionException extends RuntimeException {
    public NotAuthorizedStudentConsumptionException(UUID alumnoId) {
        super("No esta autorizado para consultar el consumo del alumno: " + alumnoId);
    }
}
