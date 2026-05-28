package com.recred.backend.domain.exceptions;

public class AlumnoRequeridoParaConsumoException extends RuntimeException {
    public AlumnoRequeridoParaConsumoException() {
        super("Debe indicar el alumno para consultar el consumo.");
    }
}
