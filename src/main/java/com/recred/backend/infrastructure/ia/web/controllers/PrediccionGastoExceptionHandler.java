package com.recred.backend.infrastructure.ia.web.controllers;

import com.recred.backend.domain.exceptions.AlumnoNoEncontradoException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class PrediccionGastoExceptionHandler {

    @ExceptionHandler(AlumnoNoEncontradoException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleAlumnoNoEncontrado(AlumnoNoEncontradoException exception) {
        return Map.of("error", exception.getMessage());
    }
}
