package com.recred.backend.config;

import com.recred.backend.control_parental.domain.exceptions.RestriccionSaludException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RestriccionSaludException.class)
    public ResponseEntity<Map<String, String>> handleRestriccionSalud(
            RestriccionSaludException ex) {
        Map<String, String> errorResponse = new HashMap<>();

        errorResponse.put("error", "Conflicto de Restricción Médica");
        errorResponse.put("mensaje", ex.getMessage());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

}