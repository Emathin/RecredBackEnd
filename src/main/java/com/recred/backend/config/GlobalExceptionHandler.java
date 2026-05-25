package com.recred.backend.config;

import com.recred.backend.domain.exceptions.PresupuestoExcedidoException;
import com.recred.backend.domain.exceptions.RestriccionHorariaException;
import com.recred.backend.domain.exceptions.RestriccionSaludException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(
            GlobalExceptionHandler.class);

    @ExceptionHandler(RestriccionHorariaException.class)
    public ResponseEntity<ErrorResponse> handleRestriccionHoraria(
            RestriccionHorariaException ex) {
        ErrorResponse error = new ErrorResponse(
                "Restricción Horaria",
                ex.getMessage(),
                LocalDateTime.now(),
                HttpStatus.FORBIDDEN.value()
        );
        return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(PresupuestoExcedidoException.class)
    public ResponseEntity<Map<String, Object>> handlePresupuestoExcedido(
            PresupuestoExcedidoException ex) {

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Límite de Presupuesto Superado");
        body.put("message", ex.getMessage());

        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(RestriccionSaludException.class)
    public ResponseEntity<ErrorResponse> handleRestriccionSalud(
            RestriccionSaludException ex) {
        return buildResponse(HttpStatus.CONFLICT, "Conflicto de Restricción Médica",
                ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst().orElse("Datos inválidos");
        return buildResponse(HttpStatus.BAD_REQUEST, "Error de validación", details);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception ex) {
        LOGGER.error("Error no controlado: ", ex);

        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno",
                "Ocurrió un error inesperado. Por favor, intente más tarde.");
    }

    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status,
                                                        String error, String message) {
        ErrorResponse response = new ErrorResponse(error, message,
                LocalDateTime.now(), status.value());
        return ResponseEntity.status(status).body(response);
    }
}