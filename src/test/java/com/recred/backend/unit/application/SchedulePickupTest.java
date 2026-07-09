package com.recred.backend.unit.application;

import com.recred.backend.application.SchedulePickup;
import com.recred.backend.application.SchedulePickupRequest;
import com.recred.backend.domain.models.CategoriaProducto;
import com.recred.backend.domain.models.FranjaHoraria;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.services.ValidacionRestriccionesService;
import com.recred.backend.domain.services.ValidadorHorarioService;
import com.recred.backend.domain.services.ValidadorPresupuestoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SchedulePickupTest {

    private static final LocalDate PICKUP_DATE = LocalDate.of(2026, 6, 1);
    private static final LocalDateTime PROJECTED_PICKUP_TIME =
            LocalDateTime.of(2026, 6, 1, 10, 0);

    private final ValidadorHorarioService timeValidator =
            mock(ValidadorHorarioService.class);
    private final ValidacionRestriccionesService healthValidator =
            mock(ValidacionRestriccionesService.class);
    private final ValidadorPresupuestoService budgetValidator =
            mock(ValidadorPresupuestoService.class);
    private final SchedulePickup useCase =
            new SchedulePickup(timeValidator, healthValidator, budgetValidator);

    private SchedulePickupRequest request;
    private RuntimeException exception;
    private UUID studentId;
    private UUID categoryId;
    private Set<Producto> cart;

    @Test
    void execute_WhenPickupIsScheduled_ValidatesPurchaseConstraints() {
        givenDefaultPickupRequest();

        whenSchedulingPickup();

        thenPurchaseConstraintsWereValidated();
        thenBudgetWasValidated(BigDecimal.ZERO, Map.of());
    }

    @Test
    void execute_WhenCartHasProducts_CalculatesTotalAndCategoryAmounts() {
        givenPickupRequestWithPricedProducts();

        whenSchedulingPickup();

        thenBudgetWasValidated(
                new BigDecimal("15.00"),
                Map.of(categoryId, new BigDecimal("10.00"))
        );
    }

    @Test
    void execute_WhenTimeValidatorFails_ThrowsException() {
        givenDefaultPickupRequest();
        givenTimeValidationFails();

        whenSchedulingPickupFails();

        thenExceptionMessageIs("Time invalid");
    }

    @Test
    void execute_WhenHealthValidatorFails_ThrowsException() {
        givenDefaultPickupRequest();
        givenHealthValidationFails();

        whenSchedulingPickupFails();

        thenExceptionMessageIs("Health restrictions violated");
    }

    @Test
    void execute_WhenBudgetValidatorFails_ThrowsException() {
        givenDefaultPickupRequest();
        givenBudgetValidationFails();

        whenSchedulingPickupFails();

        thenExceptionMessageIs("Budget exceeded");
    }

    private void givenDefaultPickupRequest() {
        studentId = randomUUID();
        cart = Set.of(new Producto());
        request = new SchedulePickupRequest(studentId, cart, pickupSlot(), PICKUP_DATE);
    }

    private void givenPickupRequestWithPricedProducts() {
        studentId = randomUUID();
        categoryId = randomUUID();
        CategoriaProducto category = category(categoryId);
        cart = Set.of(
                product(new BigDecimal("10.00"), category),
                product(null, category),
                product(new BigDecimal("5.00"), null)
        );
        request = new SchedulePickupRequest(studentId, cart, pickupSlot(), PICKUP_DATE);
    }

    private void givenTimeValidationFails() {
        doThrow(new RuntimeException("Time invalid"))
                .when(timeValidator).validarCompra(any(), eq(cart), eq(PROJECTED_PICKUP_TIME));
    }

    private void givenHealthValidationFails() {
        doThrow(new RuntimeException("Health restrictions violated"))
                .when(healthValidator).validarCompra(any(), eq(cart));
    }

    private void givenBudgetValidationFails() {
        doThrow(new RuntimeException("Budget exceeded"))
                .when(budgetValidator).validarPresupuesto(
                        eq(studentId),
                        any(),
                        any(),
                        eq(PROJECTED_PICKUP_TIME)
                );
    }

    private void whenSchedulingPickup() {
        useCase.execute(request);
    }

    private void whenSchedulingPickupFails() {
        Executable execution = () -> useCase.execute(request);
        exception = assertThrows(RuntimeException.class, execution);
    }

    private void thenPurchaseConstraintsWereValidated() {
        verify(timeValidator).validarCompra(any(), eq(cart), eq(PROJECTED_PICKUP_TIME));
        verify(healthValidator).validarCompra(any(), eq(cart));
    }

    private void thenBudgetWasValidated(
            BigDecimal expectedTotal,
            Map<UUID, BigDecimal> expectedCategoryAmounts
    ) {
        verify(budgetValidator).validarPresupuesto(
                eq(studentId),
                eq(expectedTotal),
                eq(expectedCategoryAmounts),
                eq(PROJECTED_PICKUP_TIME)
        );
    }

    private void thenExceptionMessageIs(String expectedMessage) {
        assertEquals(expectedMessage, exception.getMessage());
    }

    private FranjaHoraria pickupSlot() {
        FranjaHoraria pickupSlot = new FranjaHoraria();
        pickupSlot.setHoraInicio(LocalTime.of(10, 0));
        return pickupSlot;
    }

    private CategoriaProducto category(UUID id) {
        CategoriaProducto category = new CategoriaProducto();
        category.setId(id);
        return category;
    }

    private Producto product(BigDecimal price, CategoriaProducto category) {
        Producto product = new Producto();
        product.setPrecio(price);
        product.setCategoria(category);
        return product;
    }
}
