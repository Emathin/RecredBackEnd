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

    public void execute(SchedulePickupRequest request) {
        Alumno student = this.createAlumno(request.studentId());
        LocalDateTime projectedPickupTime = this.calculateProjectedPickupTime(
                request.pickupDate(), request.pickupSlot());

        this.validatePurchaseConstraints(student, request.cart(), projectedPickupTime);

        BigDecimal montoTotalCarrito = this.calculateTotalCartAmount(request.cart());
        Map<UUID, BigDecimal> montosPorCategoria = this.calculateAmountsByCategory(request.cart());

        this.validateBudget(request.studentId(), montoTotalCarrito,
                montosPorCategoria, projectedPickupTime);
    }

    private Alumno createAlumno(UUID studentId) {
        Alumno student = new Alumno();
        student.setId(studentId);
        return student;
    }

    private LocalDateTime calculateProjectedPickupTime(LocalDate pickupDate, FranjaHoraria pickupSlot) {
        return LocalDateTime.of(pickupDate, pickupSlot.getHoraInicio());
    }

    private void validatePurchaseConstraints(
            Alumno student, Set<Producto> cart, LocalDateTime projectedPickupTime) {
        timeValidator.validarCompra(student, cart, projectedPickupTime);
        validacionRestriccionesService.validarCompra(student, cart);
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

    private void validateBudget(
            UUID studentId,
            BigDecimal totalCartAmount,
            Map<UUID, BigDecimal> amountsByCategory,
            LocalDateTime projectedPickupTime) {
        budgetValidator.validarPresupuesto(
                studentId,
                totalCartAmount,
                amountsByCategory,
                projectedPickupTime
        );
    }
}