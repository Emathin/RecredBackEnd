package com.recred.backend.domain.exceptions;

import java.util.UUID;

public class ConsumoAlumnoNoAutorizadoException extends RuntimeException {
    public ConsumoAlumnoNoAutorizadoException(UUID alumnoId) {
        super("No esta autorizado para consultar el consumo del alumno: " + alumnoId);
    }
}
