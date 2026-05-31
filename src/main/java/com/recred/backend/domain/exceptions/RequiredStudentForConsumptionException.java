package com.recred.backend.domain.exceptions;

public class RequiredStudentForConsumptionException extends RuntimeException {
    public RequiredStudentForConsumptionException() {
        super("Debe indicar el alumno para consultar el consumo.");
    }
}
