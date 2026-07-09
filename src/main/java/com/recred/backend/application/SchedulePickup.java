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

    public SchedulePickup(
            ValidadorHorarioService timeValidator,
            ValidacionRestriccionesService validacionRestriccionesService,
            ValidadorPresupuestoService budgetValidator
    ) {
        this.timeValidator = timeValidator;
        this.validacionRestriccionesService = validacionRestriccionesService;
        this.budgetValidator = budgetValidator;
    }

    public SchedulePickupResponse execute(SchedulePickupRequest request) {
        PickupValidationContext context = validationContextFor(request);
        validatePurchaseConstraints(context);
        validateBudget(context);
        return new SchedulePickupResponse();
    }

    private PickupValidationContext validationContextFor(SchedulePickupRequest request) {
        return new PickupValidationContext(
                request.studentId(),
                createAlumno(request.studentId()),
                request.cart(),
                calculateProjectedPickupTime(request.pickupDate(), request.pickupSlot()),
                calculateTotalCartAmount(request.cart()),
                calculateAmountsByCategory(request.cart())
        );
    }

    private Alumno createAlumno(UUID studentId) {
        Alumno student = new Alumno();
        student.setId(studentId);
        return student;
    }

    private LocalDateTime calculateProjectedPickupTime(
            LocalDate pickupDate,
            FranjaHoraria pickupSlot
    ) {
        return LocalDateTime.of(pickupDate, pickupSlot.getHoraInicio());
    }

    private void validatePurchaseConstraints(PickupValidationContext context) {
        timeValidator.validarCompra(
                context.student(),
                context.cart(),
                context.projectedPickupTime()
        );
        validacionRestriccionesService.validarCompra(context.student(), context.cart());
    }

    private BigDecimal calculateTotalCartAmount(Set<Producto> cart) {
        return cart.stream()
                .map(p -> p.getPrecio() != null ? p.getPrecio() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Map<UUID, BigDecimal> calculateAmountsByCategory(Set<Producto> cart) {
        return cart.stream()
                .filter(p -> p.getCategoria() != null && p.getCategoria().getId() != null)
                .collect(Collectors.groupingBy(
                        p -> p.getCategoria().getId(),
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                p -> p.getPrecio() != null ? p.getPrecio() : BigDecimal.ZERO,
                                BigDecimal::add
                        )
                ));
    }

    private void validateBudget(PickupValidationContext context) {
        budgetValidator.validarPresupuesto(
                context.studentId(),
                context.totalCartAmount(),
                context.amountsByCategory(),
                context.projectedPickupTime()
        );
    }

    private record PickupValidationContext(
            UUID studentId,
            Alumno student,
            Set<Producto> cart,
            LocalDateTime projectedPickupTime,
            BigDecimal totalCartAmount,
            Map<UUID, BigDecimal> amountsByCategory
    ) {
    }
}
