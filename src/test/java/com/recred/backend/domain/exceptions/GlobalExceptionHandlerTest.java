package com.recred.backend.domain.exceptions;

import com.recred.backend.config.ErrorResponse;
import com.recred.backend.config.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
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
    }

    @Test
    void handlePresupuestoExcedido_RetornaBadRequestConMap() {
        PresupuestoExcedidoException ex = new PresupuestoExcedidoException("Límite superado");

        ResponseEntity<Map<String, Object>> response = exceptionHandler.handlePresupuestoExcedido(ex);
        Map<String, Object> body = response.getBody();

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(body);
        assertEquals(400, body.get("status"));
        assertEquals("Límite de Presupuesto Superado", body.get("error"));
        assertEquals("Límite superado", body.get("message"));
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