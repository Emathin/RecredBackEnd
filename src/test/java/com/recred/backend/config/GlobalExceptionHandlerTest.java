package com.recred.backend.config;

import com.recred.backend.domain.exceptions.AlumnoNoEncontradoException;
import com.recred.backend.domain.exceptions.BuffetNotFoundException;
import com.recred.backend.domain.exceptions.ChatSessionNotFoundException;
import com.recred.backend.domain.exceptions.ImageValidationException;
import com.recred.backend.domain.exceptions.InsufficientBalanceException;
import com.recred.backend.domain.exceptions.InsufficientStockException;
import com.recred.backend.domain.exceptions.InventoryNotFoundException;
import com.recred.backend.domain.exceptions.InventoryOperationException;
import com.recred.backend.domain.exceptions.InvalidChatMessageException;
import com.recred.backend.domain.exceptions.InvalidPurchaseStatusTransitionException;
import com.recred.backend.domain.exceptions.KiosqueroNotFoundException;
import com.recred.backend.domain.exceptions.PickupCutoffTimeExceededException;
import com.recred.backend.domain.exceptions.PickupSlotCapacityExceededException;
import com.recred.backend.domain.exceptions.PickupSlotRulesNotFoundException;
import com.recred.backend.domain.exceptions.PresupuestoExcedidoException;
import com.recred.backend.domain.exceptions.ProductDailyCapacityExceededException;
import com.recred.backend.domain.exceptions.ProductNotFoundException;
import com.recred.backend.domain.exceptions.PurchaseAccessDeniedException;
import com.recred.backend.domain.exceptions.PurchaseNotFoundException;
import com.recred.backend.domain.exceptions.RestriccionHorariaException;
import com.recred.backend.domain.exceptions.RestriccionSaludException;
import com.recred.backend.domain.exceptions.TutorNoEncontradoException;
import com.recred.backend.domain.models.EstadoCompra;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @AfterEach
    void clearRequestId() {
        RequestIdContext.clear();
    }

    @Test
    void handlePresupuestoExcedido_ShouldReturnBudgetRestrictionWithRequestId() {
        RequestIdContext.put("request-1");

        ResponseEntity<Map<String, Object>> response = handler.handlePresupuestoExcedido(
                new PresupuestoExcedidoException("Limite superado")
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("BUDGET_RESTRICTION", response.getBody().get("code"));
        assertEquals("request-1", response.getBody().get("requestId"));
    }

    @Test
    void handleRestriccionHoraria_ShouldReturnForbiddenResponse() {
        RequestIdContext.put("request-2");

        ResponseEntity<ErrorResponse> response = handler.handleRestriccionHoraria(
                new RestriccionHorariaException("Fuera de horario")
        );

        assertError(response, HttpStatus.FORBIDDEN, "TIME_RESTRICTION", "request-2");
    }

    @Test
    void domainHandlers_ShouldReturnExpectedStatusesAndCodes() {
        UUID id = UUID.randomUUID();
        assertError(handler.handleRestriccionSalud(new RestriccionSaludException("Salud")),
                HttpStatus.CONFLICT, "HEALTH_RESTRICTION", null);
        assertError(handler.handleAlumnoNoEncontrado(new AlumnoNoEncontradoException(id)),
                HttpStatus.NOT_FOUND, "NOT_FOUND", null);
        assertError(handler.handleInsufficientBalance(new InsufficientBalanceException("Saldo")),
                HttpStatus.BAD_REQUEST, "INSUFFICIENT_BALANCE", null);
        assertError(handler.handleInsufficientStock(new InsufficientStockException(id, 3, 1)),
                HttpStatus.CONFLICT, "STOCK_INSUFFICIENT", null);
        assertError(handler.handleInventoryNotFound(new InventoryNotFoundException(id, id)),
                HttpStatus.NOT_FOUND, "NOT_FOUND", null);
        assertError(handler.handleInventoryOperation(new InventoryOperationException("Invalido")),
                HttpStatus.BAD_REQUEST, "INVENTORY_OPERATION_INVALID", null);
        assertError(handler.handleImageValidation(new ImageValidationException("Imagen")),
                HttpStatus.BAD_REQUEST, "BAD_REQUEST", null);
        assertError(handler.handlePurchaseNotFound(new PurchaseNotFoundException(id)),
                HttpStatus.NOT_FOUND, "NOT_FOUND", null);
        assertError(handler.handlePurchaseAccessDenied(new PurchaseAccessDeniedException(id)),
                HttpStatus.FORBIDDEN, "FORBIDDEN", null);
        assertError(handler.handleInvalidPurchaseStatusTransition(
                new InvalidPurchaseStatusTransitionException(
                        EstadoCompra.PENDIENTE,
                        EstadoCompra.ENTREGADO
                )), HttpStatus.BAD_REQUEST, "INVALID_PURCHASE_STATUS_TRANSITION", null);
    }

    @Test
    void pickupAndCatalogHandlers_ShouldReturnExpectedStatusesAndCodes() {
        UUID id = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 6, 15);
        assertError(handler.handlePickupCutoffTimeExceeded(
                new PickupCutoffTimeExceededException(id, LocalDateTime.now())),
                HttpStatus.BAD_REQUEST, "CUTOFF_TIME_EXCEEDED", null);
        assertError(handler.handlePickupSlotCapacityExceeded(
                new PickupSlotCapacityExceededException(id, date, 5)),
                HttpStatus.CONFLICT, "TIME_SLOT_CAPACITY_EXCEEDED", null);
        assertError(handler.handlePickupSlotRulesNotFound(
                new PickupSlotRulesNotFoundException(id)),
                HttpStatus.NOT_FOUND, "TIME_SLOT_NOT_FOUND", null);
        assertError(handler.handleProductDailyCapacity(
                new ProductDailyCapacityExceededException(id, date, 5)),
                HttpStatus.CONFLICT, "PRODUCT_DAILY_CAPACITY_EXCEEDED", null);
        assertError(handler.handleBuffetNotFound(new BuffetNotFoundException(id)),
                HttpStatus.NOT_FOUND, "BUFFET_NOT_FOUND", null);
        assertError(handler.handleProductNotFound(new ProductNotFoundException(id)),
                HttpStatus.NOT_FOUND, "NOT_FOUND", null);
        assertError(handler.handleTutorNoEncontrado(new TutorNoEncontradoException("sub")),
                HttpStatus.NOT_FOUND, "NOT_FOUND", null);
        assertError(handler.handleKiosqueroNotFound(new KiosqueroNotFoundException(id)),
                HttpStatus.NOT_FOUND, "NOT_FOUND", null);
    }

    @Test
    void genericHandlers_ShouldReturnExpectedStatuses() {
        UUID id = UUID.randomUUID();
        assertError(handler.handleInvalidChatMessage(new InvalidChatMessageException()),
                HttpStatus.BAD_REQUEST, "BAD_REQUEST", null);
        assertError(handler.handleChatSessionNotFound(new ChatSessionNotFoundException(id)),
                HttpStatus.NOT_FOUND, "NOT_FOUND", null);
        assertError(handler.handleIllegalArgument(new IllegalArgumentException("Invalido")),
                HttpStatus.BAD_REQUEST, "BAD_REQUEST", null);
        assertError(handler.handleAccessDenied(new AccessDeniedException("Denegado")),
                HttpStatus.FORBIDDEN, "FORBIDDEN", null);
        assertError(handler.handleDataIntegrityViolation(
                new DataIntegrityViolationException("Duplicado")),
                HttpStatus.CONFLICT, "CONFLICT", null);
        assertError(handler.handleGeneralException(new RuntimeException("Error")),
                HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", null);
    }

    private void assertError(
            ResponseEntity<ErrorResponse> response,
            HttpStatus status,
            String code,
            String requestId
    ) {
        assertEquals(status, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(status.value(), response.getBody().status());
        assertEquals(code, response.getBody().code());
        assertEquals(requestId, response.getBody().requestId());
    }
}
