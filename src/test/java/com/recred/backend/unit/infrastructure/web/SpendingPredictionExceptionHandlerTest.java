package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.domain.exceptions.AlumnoNoEncontradoException;
import java.util.Map;
import java.util.UUID;

import com.recred.backend.infrastructure.web.SpendingPredictionExceptionHandler;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SpendingPredictionExceptionHandlerTest {

    @Test
    void givenAlumnoNoEncontradoExceptionWhenHandlingThenReturnsErrorResponse() {
        UUID studentId = UUID.randomUUID();
        SpendingPredictionExceptionHandler handler = new SpendingPredictionExceptionHandler();

        Map<String, String> response = handler.handleAlumnoNoEncontrado(
                new AlumnoNoEncontradoException(studentId)
        );

        thenResponseHasError(response, "No existe el alumno con id " + studentId);
    }

    private void thenResponseHasError(Map<String, String> response, String errorMessage) {
        assertEquals(errorMessage, response.get("error"));
    }
}

