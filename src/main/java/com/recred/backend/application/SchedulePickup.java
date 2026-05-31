package com.recred.backend.application;

import com.recred.backend.domain.models.Product;
import com.recred.backend.domain.models.Student;
import com.recred.backend.domain.models.TimeBadge;
import com.recred.backend.domain.services.RestrictionsValidatorService;
import com.recred.backend.domain.services.TimeValidatorService;
import com.recred.backend.domain.services.BudgetValidatorService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class SchedulePickup {

    private final TimeValidatorService timeValidator;
    private final RestrictionsValidatorService restrictionsValidatorService;
    private final BudgetValidatorService budgetValidator;

    public SchedulePickup(TimeValidatorService timeValidator,
                          RestrictionsValidatorService restrictionsValidatorService,
                          BudgetValidatorService budgetValidator) {
        this.timeValidator = timeValidator;
        this.restrictionsValidatorService = restrictionsValidatorService;
        this.budgetValidator = budgetValidator;
    }

    public void execute(UUID studentId, Set<Product> cart,
                        TimeBadge pickupSlot,
                        LocalDate pickupDate) {

        Student student = new Student();
        student.setId(studentId);

        LocalDateTime projectedPickupTime = LocalDateTime.of(pickupDate,
                pickupSlot.getStartTime());

        timeValidator.validatePurchase(student, cart, projectedPickupTime);
        restrictionsValidatorService.validatePurchase(student, cart);

        BigDecimal montoTotalCarrito = cart.stream()
                .map(p -> p.getPrice() != null ? p.getPrice() :
                        BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<UUID, BigDecimal> montosPorCategoria = cart.stream()
                .filter(p -> p.getCategory() != null &&
                        p.getCategory().getId() != null)
                .collect(Collectors.groupingBy(
                        p -> p.getCategory().getId(),
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                p -> p.getPrice() != null ?
                                        p.getPrice() : BigDecimal.ZERO,
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