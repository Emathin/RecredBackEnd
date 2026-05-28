package com.recred.backend.infrastructure.web;

import com.recred.backend.domain.exceptions.AlumnoNoEncontradoException;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SpendingPredictionExceptionHandlerTest {

    @Test
    void mapearAlumnoNoEncontradoARespuestaDeError() {
        UUID studentId = UUID.randomUUID();
        SpendingPredictionExceptionHandler handler = new SpendingPredictionExceptionHandler();

        Map<String, String> response = handler.handleAlumnoNoEncontrado(
                new AlumnoNoEncontradoException(studentId)
        );

        assertEquals("No existe el alumno con id " + studentId, response.get("error"));
    }
}
