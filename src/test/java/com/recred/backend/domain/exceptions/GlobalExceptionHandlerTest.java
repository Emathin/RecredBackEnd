package com.recred.backend.domain.exceptions;

import com.recred.backend.config.ErrorResponse;
import com.recred.backend.config.GlobalExceptionHandler;
import com.recred.backend.config.RequestIdContext;
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
    void handleRestriccionHoraria_RetornaForbidden() {
        RestriccionHorariaException ex = new RestriccionHorariaException("Fuera de horario");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleRestriccionHoraria(ex);
        ErrorResponse body = response.getBody();

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(body);
        assertEquals("Restricción Horaria", body.error());
        assertEquals("Fuera de horario", body.mensaje());
        assertEquals(403, body.status());
        assertEquals(REQUEST_ID, body.requestId());
    }

    @Test
    void handlePresupuestoExcedido_RetornaBadRequestConMap() {
        PresupuestoExcedidoException ex = new PresupuestoExcedidoException("Límite superado");

        ResponseEntity<Map<String, Object>> response =
                exceptionHandler.handlePresupuestoExcedido(ex);
        Map<String, Object> body = response.getBody();

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(body);
        assertEquals(400, body.get("status"));
        assertEquals("Límite de Presupuesto Superado", body.get("error"));
        assertEquals("Límite superado", body.get("message"));
        assertEquals(REQUEST_ID, body.get("requestId"));
        assertTrue(body.get("timestamp") instanceof LocalDateTime);
    }

    @Test
    void handleRestriccionSalud_RetornaConflict() {
        RestriccionSaludException ex = new RestriccionSaludException("El alumno es celíaco");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleRestriccionSalud(ex);
        ErrorResponse body = response.getBody();

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(body);
        assertEquals("Conflicto de Restricción Médica", body.error());
        assertEquals("El alumno es celíaco", body.mensaje());
        assertEquals(409, body.status());
    }

    @Test
    void handleAlumnoNoEncontrado_RetornaNotFound() {
        AlumnoNoEncontradoException ex = new AlumnoNoEncontradoException(UUID.randomUUID());

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleAlumnoNoEncontrado(ex);
        ErrorResponse body = response.getBody();

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(body);
        assertEquals("Alumno no encontrado", body.error());
    }

    @Test
    void handleInsufficientBalance_RetornaBadRequest() {
        InsufficientBalanceException ex = new InsufficientBalanceException("Saldo bajo");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleInsufficientBalance(ex);
        ErrorResponse body = response.getBody();

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(body);
        assertEquals("Saldo Insuficiente", body.error());
        assertEquals("Saldo bajo", body.mensaje());
    }

    @Test
    void handleInvalidChatMessage_RetornaBadRequest() {
        InvalidChatMessageException ex = new InvalidChatMessageException();

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleInvalidChatMessage(ex);
        ErrorResponse body = response.getBody();

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(body);
        assertEquals("Mensaje invalido", body.error());
    }

    @Test
    void handleChatSessionNotFound_RetornaNotFound() {
        ChatSessionNotFoundException ex = new ChatSessionNotFoundException(UUID.randomUUID());

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleChatSessionNotFound(ex);
        ErrorResponse body = response.getBody();

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(body);
        assertEquals("Sesion de chat no encontrada", body.error());
    }

    @Test
    void handleValidation_ConErroresDeCampo_ExtraeElPrimerError() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("objeto", "nombre", "El nombre es obligatorio");

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleValidation(ex);
        ErrorResponse body = response.getBody();

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(body);
        assertEquals("Error de validación", body.error());
        assertEquals("nombre: El nombre es obligatorio", body.mensaje());
    }

    @Test
    void handleValidation_SinErroresDeCampo_UsaMensajePorDefecto() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of());

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleValidation(ex);
        ErrorResponse body = response.getBody();

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(body);
        assertEquals("Error de validación", body.error());
        assertEquals("Datos inválidos", body.mensaje());
    }

    @Test
    void handlePurchaseNotFound_RetornaNotFound() {
        PurchaseNotFoundException ex = new PurchaseNotFoundException(UUID.randomUUID());

        ResponseEntity<ErrorResponse> response = exceptionHandler.handlePurchaseNotFound(ex);
        ErrorResponse body = response.getBody();

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(body);
        assertEquals("Compra no encontrada", body.error());
    }

    @Test
    void handlePurchaseAccessDenied_RetornaForbidden() {
        PurchaseAccessDeniedException ex = new PurchaseAccessDeniedException(UUID.randomUUID());

        ResponseEntity<ErrorResponse> response = exceptionHandler.handlePurchaseAccessDenied(ex);
        ErrorResponse body = response.getBody();

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(body);
        assertEquals("Compra no autorizada", body.error());
    }

    @Test
    void handleInvalidPurchaseStatusTransition_RetornaBadRequest() {
        InvalidPurchaseStatusTransitionException ex =
                new InvalidPurchaseStatusTransitionException(
                        EstadoCompra.PENDIENTE,
                        EstadoCompra.LISTO
                );

        ResponseEntity<ErrorResponse> response =
                exceptionHandler.handleInvalidPurchaseStatusTransition(ex);
        ErrorResponse body = response.getBody();

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(body);
        assertEquals("Estado de compra invalido", body.error());
    }

    @Test
    void handleIllegalArgument_RetornaBadRequest() {
        IllegalArgumentException ex = new IllegalArgumentException("Dato invalido");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleIllegalArgument(ex);
        ErrorResponse body = response.getBody();

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(body);
        assertEquals("Regla de Negocio Inv\u00e1lida", body.error());
        assertEquals("Dato invalido", body.mensaje());
    }

    @Test
    void handleDataIntegrityViolation_RetornaConflict() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException("duplicado");

        ResponseEntity<ErrorResponse> response =
                exceptionHandler.handleDataIntegrityViolation(ex);
        ErrorResponse body = response.getBody();

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(body);
        assertEquals("Conflicto de Datos", body.error());
    }

    @Test
    void handleGeneralException_RetornaInternalServerError() {
        Exception ex = new Exception("Error en la base de datos");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleGeneralException(ex);
        ErrorResponse body = response.getBody();

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(body);
        assertEquals("Error interno", body.error());
        assertEquals("Ocurrió un error inesperado. Por favor, intente más tarde.", body.mensaje());
        assertEquals(500, body.status());
    }
}
