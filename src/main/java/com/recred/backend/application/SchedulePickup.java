package com.recred.backend.application;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.FranjaHoraria;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.services.BudgetValidatorService;
import com.recred.backend.domain.services.HealthRestrictionValidatorService;
import com.recred.backend.domain.services.TimeRestrictionValidatorService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class SchedulePickup {

    private final TimeRestrictionValidatorService timeValidator;
    private final HealthRestrictionValidatorService healthRestrictionValidatorService;
    private final BudgetValidatorService budgetValidator;

    public SchedulePickup(TimeRestrictionValidatorService timeValidator,
                          HealthRestrictionValidatorService healthRestrictionValidatorService,
                          BudgetValidatorService budgetValidator) {
        this.timeValidator = timeValidator;
        this.healthRestrictionValidatorService = healthRestrictionValidatorService;
        this.budgetValidator = budgetValidator;
    }

    public void execute(UUID studentId, Set<Producto> cart,
                        FranjaHoraria pickupSlot,
                        LocalDate pickupDate) {

        Alumno student = new Alumno();
        student.setId(studentId);

        LocalDateTime projectedPickupTime = LocalDateTime.of(pickupDate,
                pickupSlot.getHoraInicio());

        timeValidator.validatePurchase(student, cart, projectedPickupTime);
        healthRestrictionValidatorService.validatePurchase(student, cart);

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

        budgetValidator.validateBudget(
                studentId,
                montoTotalCarrito,
                montosPorCategoria,
                projectedPickupTime
        );

    }
}