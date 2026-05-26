package com.recred.backend.application;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.FranjaHoraria;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.services.ValidacionRestriccionesService;
import com.recred.backend.domain.services.ValidadorHorarioService;
import com.recred.backend.domain.services.ValidadorPresupuestoService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class SchedulePickup {

    private final ValidadorHorarioService timeValidator;
    private final ValidacionRestriccionesService validacionRestriccionesService;
    private final ValidadorPresupuestoService budgetValidator;

    public SchedulePickup(ValidadorHorarioService timeValidator,
                          ValidacionRestriccionesService validacionRestriccionesService,
                          ValidadorPresupuestoService budgetValidator) {
        this.timeValidator = timeValidator;
        this.validacionRestriccionesService = validacionRestriccionesService;
        this.budgetValidator = budgetValidator;
    }

    public void execute(UUID studentId, Set<Producto> cart,
                        FranjaHoraria pickupSlot,
                        LocalDate pickupDate) {

        Alumno student = new Alumno();
        student.setId(studentId);

        LocalDateTime projectedPickupTime = LocalDateTime.of(pickupDate,
                pickupSlot.getHoraInicio());

        timeValidator.validarCompra(student, cart, projectedPickupTime);
        validacionRestriccionesService.validarCompra(student, cart);

        BigDecimal montoTotalCarrito = cart.stream()
                .map(p -> p.getPrecio() != null ? p.getPrecio() :
                        BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<UUID, BigDecimal> montosPorCategoria = cart.stream()
                .filter(p -> p.getCategoria() != null &&
                        p.getCategoria().getId() != null)
                .collect(Collectors.groupingBy(
                        p -> p.getCategoria().getId(),
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                p -> p.getPrecio() != null ?
                                        p.getPrecio() : BigDecimal.ZERO,
                                BigDecimal::add
                        )
                ));

        budgetValidator.validarPresupuesto(
                studentId,
                montoTotalCarrito,
                montosPorCategoria,
                projectedPickupTime
        );

    }
}