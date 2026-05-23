package com.recred.backend.infrastructure.ia.web.controllers;

import com.recred.backend.domain.exceptions.AlumnoNoEncontradoException;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PrediccionGastoExceptionHandlerTest {

    @Test
    void mapearAlumnoNoEncontradoARespuestaDeError() {
        UUID alumnoId = UUID.randomUUID();
        PrediccionGastoExceptionHandler handler = new PrediccionGastoExceptionHandler();

        Map<String, String> response = handler.handleAlumnoNoEncontrado(
                new AlumnoNoEncontradoException(alumnoId)
        );

        assertEquals("No existe el alumno con id " + alumnoId, response.get("error"));
    }
}
