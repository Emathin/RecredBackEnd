package com.recred.backend.config;

import com.recred.backend.domain.exceptions.AlumnoNoEncontradoException;
import com.recred.backend.domain.exceptions.ImageValidationException;
import com.recred.backend.domain.exceptions.ChatSessionNotFoundException;
import com.recred.backend.domain.exceptions.InvalidPurchaseStatusTransitionException;
import com.recred.backend.domain.exceptions.KiosqueroNotFoundException;
import com.recred.backend.domain.exceptions.ProductNotFoundException;
import com.recred.backend.domain.exceptions.InsufficientBalanceException;
import com.recred.backend.domain.exceptions.InvalidChatMessageException;
import com.recred.backend.domain.exceptions.PresupuestoExcedidoException;
import com.recred.backend.domain.exceptions.PurchaseAccessDeniedException;
import com.recred.backend.domain.exceptions.PurchaseNotFoundException;
import com.recred.backend.domain.exceptions.RestriccionHorariaException;
import com.recred.backend.domain.exceptions.RestriccionSaludException;
import com.recred.backend.domain.exceptions.TutorNoEncontradoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

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
                HttpStatus.FORBIDDEN.value(),
                currentRequestId()
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
        addRequestId(body);

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

    @ExceptionHandler(ImageValidationException.class)
    public ResponseEntity<ErrorResponse> handleImageValidation(ImageValidationException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Error de validación de imagen",
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

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleProductNotFound(ProductNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, "Producto no encontrado",
                ex.getMessage());
    }

    @ExceptionHandler(TutorNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> handleTutorNoEncontrado(
            TutorNoEncontradoException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, "Tutor no encontrado",
                ex.getMessage());
    }

    @ExceptionHandler(KiosqueroNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleKiosqueroNotFound(
            KiosqueroNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, "Kiosquero no encontrado",
                ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
        MethodArgumentTypeMismatchException ex) {
        String message = String.format("El parámetro '%s' recibió un valor inválido: '%s'",
                ex.getName(), ex.getValue());
        return buildResponse(HttpStatus.BAD_REQUEST, "Parámetro inválido", message);
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

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(NoResourceFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, "Recurso no encontrado",
                "El recurso solicitado no existe: " + ex.getResourcePath());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation
            (DataIntegrityViolationException ex) {
        LOGGER.error("Violación de integridad de datos: ", ex);

        String mensaje = "No se pudo guardar la información porque entra en conflicto "
                + "con los datos existentes.";
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
                LocalDateTime.now(), status.value(), currentRequestId());
        return ResponseEntity.status(status).body(response);
    }

    private String currentRequestId() {
        return RequestIdContext.currentRequestId().orElse(null);
    }

    private void addRequestId(Map<String, Object> body) {
        RequestIdContext.currentRequestId()
                .ifPresent(requestId -> body.put("requestId", requestId));
    }
}
