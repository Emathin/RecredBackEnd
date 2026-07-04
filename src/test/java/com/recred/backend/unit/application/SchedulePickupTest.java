package com.recred.backend.unit.application;

import com.recred.backend.application.SchedulePickup;
import com.recred.backend.application.SchedulePickupRequest;
import com.recred.backend.domain.models.CategoriaProducto;
import com.recred.backend.domain.models.FranjaHoraria;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.services.ValidadorHorarioService;
import com.recred.backend.domain.services.ValidacionRestriccionesService;
import com.recred.backend.domain.services.ValidadorPresupuestoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SchedulePickupTest {

    @Mock
    private ValidadorHorarioService timeValidator;

    @Mock
    private ValidacionRestriccionesService healthValidator;

    @Mock
    private ValidadorPresupuestoService budgetValidator;

    @InjectMocks
    private SchedulePickup schedulePickup;

    @Test
    void execute_EnsamblaFechaYHoraYLlamaAValidadores() {
        UUID studentId = UUID.randomUUID();
        Set<Producto> cart = Set.of(new Producto());

        LocalDate pickupDate = LocalDate.of(2026, 6, 1);
        FranjaHoraria pickupSlot = new FranjaHoraria();
        pickupSlot.setHoraInicio(LocalTime.of(10, 0));
        SchedulePickupRequest request = new SchedulePickupRequest(
                studentId,
                cart,
                pickupSlot,
                pickupDate
        );

        schedulePickup.execute(request);

        LocalDateTime expectedTime = LocalDateTime.of(2026, 6, 1, 10, 0);

        verify(timeValidator).validarCompra(any(), eq(cart), eq(expectedTime));

        verify(healthValidator).validarCompra(any(), eq(cart));

        verify(budgetValidator).validarPresupuesto(
                eq(studentId),
                eq(java.math.BigDecimal.ZERO),
                eq(java.util.Collections.emptyMap()),
                eq(expectedTime)
        );
    }

    @Test
    void execute_CalculaMontoTotalYMontosPorCategoria() {
        UUID studentId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        CategoriaProducto category = new CategoriaProducto();
        category.setId(categoryId);
        Producto productWithCategory = product(new BigDecimal("10.00"), category);
        Producto freeProductWithCategory = product(null, category);
        Producto productWithoutCategory = product(new BigDecimal("5.00"), null);
        LocalDate pickupDate = LocalDate.of(2026, 6, 1);
        FranjaHoraria pickupSlot = new FranjaHoraria();
        pickupSlot.setHoraInicio(LocalTime.of(10, 0));
        SchedulePickupRequest request = new SchedulePickupRequest(
                studentId,
                Set.of(productWithCategory, freeProductWithCategory, productWithoutCategory),
                pickupSlot,
                pickupDate
        );

        schedulePickup.execute(request);

        verify(budgetValidator).validarPresupuesto(
                eq(studentId),
                eq(new BigDecimal("15.00")),
                eq(Map.of(categoryId, new BigDecimal("10.00"))),
                eq(LocalDateTime.of(2026, 6, 1, 10, 0))
        );
    }

    private Producto product(BigDecimal price, CategoriaProducto category) {
        Producto product = new Producto();
        product.setPrecio(price);
        product.setCategoria(category);
        return product;
    }

    @Test
    void execute_CuandoTimeValidatorFalla_LanzaExcepcion() {
        UUID studentId = UUID.randomUUID();
        Set<Producto> cart = Set.of(new Producto());
        LocalDate pickupDate = LocalDate.of(2026, 6, 1);
        FranjaHoraria pickupSlot = new FranjaHoraria();
        pickupSlot.setHoraInicio(LocalTime.of(10, 0));
        LocalDateTime expectedTime = LocalDateTime.of(2026, 6, 1, 10, 0);
        SchedulePickupRequest request = new SchedulePickupRequest(
                studentId,
                cart,
                pickupSlot,
                pickupDate
        );

        doThrow(new RuntimeException("Time invalid"))
                .when(timeValidator).validarCompra(any(), eq(cart), eq(expectedTime));

        Executable execution = () -> schedulePickup.execute(request);

        assertThrows(RuntimeException.class, execution);
    }

    @Test
    void execute_CuandoHealthValidatorFalla_LanzaExcepcion() {
        UUID studentId = UUID.randomUUID();
        Set<Producto> cart = Set.of(new Producto());
        LocalDate pickupDate = LocalDate.of(2026, 6, 1);
        FranjaHoraria pickupSlot = new FranjaHoraria();
        pickupSlot.setHoraInicio(LocalTime.of(10, 0));
        SchedulePickupRequest request = new SchedulePickupRequest(
                studentId,
                cart,
                pickupSlot,
                pickupDate
        );

        doThrow(new RuntimeException("Health restrictions violated"))
                .when(healthValidator).validarCompra(any(), eq(cart));

        Executable execution = () -> schedulePickup.execute(request);

        assertThrows(RuntimeException.class, execution);
    }

    @Test
    void execute_CuandoBudgetValidatorFalla_LanzaExcepcion() {
        UUID studentId = UUID.randomUUID();
        Set<Producto> cart = Set.of(new Producto());
        LocalDate pickupDate = LocalDate.of(2026, 6, 1);
        FranjaHoraria pickupSlot = new FranjaHoraria();
        pickupSlot.setHoraInicio(LocalTime.of(10, 0));
        LocalDateTime expectedTime = LocalDateTime.of(2026, 6, 1, 10, 0);
        SchedulePickupRequest request = new SchedulePickupRequest(
                studentId,
                cart,
                pickupSlot,
                pickupDate
        );

        doThrow(new RuntimeException("Budget exceeded"))
                .when(budgetValidator).validarPresupuesto(eq(studentId), any(), any(), eq(expectedTime));

        Executable execution = () -> schedulePickup.execute(request);

        assertThrows(RuntimeException.class, execution);
    }
}
