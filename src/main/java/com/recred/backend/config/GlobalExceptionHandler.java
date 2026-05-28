package com.recred.backend.config;

import com.recred.backend.domain.exceptions.AlumnoNoEncontradoException;
import com.recred.backend.domain.exceptions.ChatSessionNotFoundException;
import com.recred.backend.domain.exceptions.InvalidPurchaseStatusTransitionException;
import com.recred.backend.domain.exceptions.InsufficientBalanceException;
import com.recred.backend.domain.exceptions.InvalidChatMessageException;
import com.recred.backend.domain.exceptions.PresupuestoExcedidoException;
import com.recred.backend.domain.exceptions.PurchaseAccessDeniedException;
import com.recred.backend.domain.exceptions.PurchaseNotFoundException;
import com.recred.backend.domain.exceptions.RestriccionHorariaException;
import com.recred.backend.domain.exceptions.RestriccionSaludException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
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
    public ResponseEntity<ErrorResponse>
    handleRestriccionHoraria(RestriccionHorariaException ex){
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

    @ExceptionHandler(AlumnoNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> handleAlumnoNoEncontrado(AlumnoNoEncontradoException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, "Alumno no encontrado",
                ex.getMessage());
    }

    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<ErrorResponse>
    handleInsufficientBalance(InsufficientBalanceException ex){
        return buildResponse(HttpStatus.BAD_REQUEST, "Saldo Insuficiente",
                ex.getMessage());
    }

    @ExceptionHandler(PurchaseNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePurchaseNotFound(PurchaseNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, "Compra no encontrada", ex.getMessage());
    }

    @ExceptionHandler(PurchaseAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handlePurchaseAccessDenied(
            PurchaseAccessDeniedException ex
    ) {
        return buildResponse(HttpStatus.FORBIDDEN, "Compra no autorizada", ex.getMessage());
    }

    @ExceptionHandler(InvalidPurchaseStatusTransitionException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPurchaseStatusTransition(
            InvalidPurchaseStatusTransitionException ex
    ) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Estado de compra invalido",
                ex.getMessage());
    }

    @ExceptionHandler(InvalidChatMessageException.class)
    public ResponseEntity<ErrorResponse> handleInvalidChatMessage(
            InvalidChatMessageException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Mensaje invalido", ex.getMessage());
    }

    @ExceptionHandler(ChatSessionNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleChatSessionNotFound(
            ChatSessionNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, "Sesion de chat no encontrada",
                ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse>
    handleValidation(MethodArgumentNotValidException ex){
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst().orElse("Datos inválidos");
        return buildResponse(HttpStatus.BAD_REQUEST, "Error de validación", details);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Regla de Negocio Inválida",
                ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation
            (DataIntegrityViolationException ex) {
        LOGGER.error("Violación de integridad de datos: ", ex);

        String mensaje = "Error al guardar la configuración. Es posible que " +
                "esté intentando asignar la misma categoría más de una vez.";
        return buildResponse(HttpStatus.CONFLICT, "Conflicto de Datos", mensaje);
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
