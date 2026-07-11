package com.recred.backend.unit.infrastructure.web;

import com.recred.backend.config.ErrorResponse;
import com.recred.backend.config.GlobalExceptionHandler;
import com.recred.backend.config.RequestIdContext;
import com.recred.backend.domain.exceptions.*;
import com.recred.backend.domain.models.EstadoCompra;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private static final String REQUEST_ID = "test-request-id";
    private GlobalExceptionHandler exceptionHandler;
    private ResponseEntity<ErrorResponse> response;
    private ResponseEntity<Map<String, Object>> mapResponse;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
        RequestIdContext.put(REQUEST_ID);
    }

    @AfterEach
    void tearDown() {
        RequestIdContext.clear();
    }

    @Test
    void handlingRestriccionHorariaShouldReturnForbidden() {
        RestriccionHorariaException ex = new RestriccionHorariaException("Fuera de horario");
        whenHandlingRestriccionHoraria(ex);
        thenResponseIsForbidden("Restricción Horaria", "Fuera de horario");
    }

    @Test
    void handlingPresupuestoExcedidoShouldReturnBadRequest() {
        PresupuestoExcedidoException ex = new PresupuestoExcedidoException("Límite superado");
        whenHandlingPresupuestoExcedido(ex);
        thenResponseIsBadRequestMap("Límite de Presupuesto Superado", "Límite superado");
    }

    @Test
    void handlingRestriccionSaludShouldReturnConflict() {
        RestriccionSaludException ex = new RestriccionSaludException("El alumno es celíaco");
        whenHandlingRestriccionSalud(ex);
        thenResponseIsConflict("Conflicto de Restricción Médica", "El alumno es celíaco");
    }

    @Test
    void handlingAlumnoNoEncontradoShouldReturnNotFound() {
        AlumnoNoEncontradoException ex = new AlumnoNoEncontradoException(UUID.randomUUID());
        whenHandlingAlumnoNoEncontrado(ex);
        thenResponseIsNotFound("Alumno no encontrado");
    }

    @Test
    void handlingInsufficientBalanceShouldReturnBadRequest() {
        InsufficientBalanceException ex = new InsufficientBalanceException("Saldo bajo");
        whenHandlingInsufficientBalance(ex);
        thenResponseIsBadRequest("Saldo Insuficiente", "Saldo bajo");
    }

    @Test
    void handlingInvalidChatMessageShouldReturnBadRequest() {
        InvalidChatMessageException ex = new InvalidChatMessageException();
        whenHandlingInvalidChatMessage(ex);
        thenResponseIsBadRequest("Mensaje invalido");
    }

    @Test
    void handlingChatSessionNotFoundShouldReturnNotFound() {
        ChatSessionNotFoundException ex = new ChatSessionNotFoundException(UUID.randomUUID());
        whenHandlingChatSessionNotFound(ex);
        thenResponseIsNotFound("Sesion de chat no encontrada");
    }

    @Test
    void handlingValidationWithErrorsShouldExtractFirstError() {
        MethodArgumentNotValidException ex = givenValidationExceptionWithErrors("objeto", "nombre", "El nombre es obligatorio");
        whenHandlingValidation(ex);
        thenResponseIsBadRequest("Error de validación", "nombre: El nombre es obligatorio");
    }

    @Test
    void handlingValidationWithoutErrorsShouldUseDefaultMessage() {
        MethodArgumentNotValidException ex = givenValidationExceptionWithoutErrors();
        whenHandlingValidation(ex);
        thenResponseIsBadRequest("Error de validación", "Datos inválidos");
    }

    @Test
    void handlingPurchaseNotFoundShouldReturnNotFound() {
        PurchaseNotFoundException ex = new PurchaseNotFoundException(UUID.randomUUID());
        whenHandlingPurchaseNotFound(ex);
        thenResponseIsNotFound("Compra no encontrada");
    }

    @Test
    void handlingPurchaseAccessDeniedShouldReturnForbidden() {
        PurchaseAccessDeniedException ex = new PurchaseAccessDeniedException(UUID.randomUUID());
        whenHandlingPurchaseAccessDenied(ex);
        thenResponseIsForbidden("Compra no autorizada", null);
    }

    @Test
    void handlingInvalidPurchaseStatusShouldReturnBadRequest() {
        InvalidPurchaseStatusTransitionException ex = new InvalidPurchaseStatusTransitionException(EstadoCompra.PENDIENTE, EstadoCompra.LISTO);
        whenHandlingInvalidPurchaseStatusTransition(ex);
        thenResponseIsBadRequest("Estado de compra invalido");
    }

    @Test
    void handlingIllegalArgumentShouldReturnBadRequest() {
        IllegalArgumentException ex = new IllegalArgumentException("Dato invalido");
        whenHandlingIllegalArgument(ex);
        thenResponseIsBadRequest("Regla de Negocio Inválida", "Dato invalido");
    }

    @Test
    void handlingDataIntegrityViolationShouldReturnConflict() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException("duplicado");
        whenHandlingDataIntegrityViolation(ex);
        thenResponseIsConflict("Conflicto de Datos", "No se pudo guardar la información porque entra en conflicto con los datos existentes.");
    }

    @Test
    void handlingGeneralExceptionShouldReturnInternalServerError() {
        Exception ex = new Exception("Error en la base de datos");
        whenHandlingGeneralException(ex);
        thenResponseIsInternalServerError("Error interno", "Ocurrió un error inesperado. Por favor, intente más tarde.");
    }

    private MethodArgumentNotValidException givenValidationExceptionWithErrors(String objectName, String field, String defaultMessage) {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError(objectName, field, defaultMessage);
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));
        return ex;
    }

    private MethodArgumentNotValidException givenValidationExceptionWithoutErrors() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of());
        return ex;
    }

    private void whenHandlingRestriccionHoraria(RestriccionHorariaException ex) {
        response = exceptionHandler.handleRestriccionHoraria(ex);
    }

    private void whenHandlingPresupuestoExcedido(PresupuestoExcedidoException ex) {
        mapResponse = exceptionHandler.handlePresupuestoExcedido(ex);
    }

    private void whenHandlingRestriccionSalud(RestriccionSaludException ex) {
        response = exceptionHandler.handleRestriccionSalud(ex);
    }

    private void whenHandlingAlumnoNoEncontrado(AlumnoNoEncontradoException ex) {
        response = exceptionHandler.handleAlumnoNoEncontrado(ex);
    }

    private void whenHandlingInsufficientBalance(InsufficientBalanceException ex) {
        response = exceptionHandler.handleInsufficientBalance(ex);
    }

    private void whenHandlingInvalidChatMessage(InvalidChatMessageException ex) {
        response = exceptionHandler.handleInvalidChatMessage(ex);
    }

    private void whenHandlingChatSessionNotFound(ChatSessionNotFoundException ex) {
        response = exceptionHandler.handleChatSessionNotFound(ex);
    }

    private void whenHandlingValidation(MethodArgumentNotValidException ex) {
        response = exceptionHandler.handleValidation(ex);
    }

    private void whenHandlingPurchaseNotFound(PurchaseNotFoundException ex) {
        response = exceptionHandler.handlePurchaseNotFound(ex);
    }

    private void whenHandlingPurchaseAccessDenied(PurchaseAccessDeniedException ex) {
        response = exceptionHandler.handlePurchaseAccessDenied(ex);
    }

    private void whenHandlingInvalidPurchaseStatusTransition(InvalidPurchaseStatusTransitionException ex) {
        response = exceptionHandler.handleInvalidPurchaseStatusTransition(ex);
    }

    private void whenHandlingIllegalArgument(IllegalArgumentException ex) {
        response = exceptionHandler.handleIllegalArgument(ex);
    }

    private void whenHandlingDataIntegrityViolation(DataIntegrityViolationException ex) {
        response = exceptionHandler.handleDataIntegrityViolation(ex);
    }

    private void whenHandlingGeneralException(Exception ex) {
        response = exceptionHandler.handleGeneralException(ex);
    }

    private void thenResponseIsForbidden(String error, String message) {
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(error, response.getBody().error());
        assertEquals(403, response.getBody().status());
        if (message != null) {
            assertEquals(message, response.getBody().mensaje());
            assertEquals(REQUEST_ID, response.getBody().requestId());
        }
    }

    private void thenResponseIsBadRequestMap(String error, String message) {
        assertEquals(HttpStatus.BAD_REQUEST, mapResponse.getStatusCode());
        Map<String, Object> body = mapResponse.getBody();
        assertNotNull(body);
        assertEquals(400, body.get("status"));
        assertEquals(error, body.get("error"));
        assertEquals(message, body.get("message"));
        assertEquals(REQUEST_ID, body.get("requestId"));
        assertTrue(body.get("timestamp") instanceof LocalDateTime);
    }

    private void thenResponseIsConflict(String error, String message) {
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(error, response.getBody().error());
        assertEquals(message, response.getBody().mensaje());
        assertEquals(409, response.getBody().status());
    }

    private void thenResponseIsNotFound(String error) {
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(error, response.getBody().error());
    }

    private void thenResponseIsBadRequest(String error, String message) {
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(error, response.getBody().error());
        assertEquals(message, response.getBody().mensaje());
    }

    private void thenResponseIsBadRequest(String error) {
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(error, response.getBody().error());
    }

    private void thenResponseIsInternalServerError(String error, String message) {
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(error, response.getBody().error());
        assertEquals(message, response.getBody().mensaje());
        assertEquals(500, response.getBody().status());
    }
}
