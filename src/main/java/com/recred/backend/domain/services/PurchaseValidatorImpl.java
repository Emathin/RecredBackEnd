package com.recred.backend.domain.services;

import com.recred.backend.domain.models.Purchase;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;


public class PurchaseValidatorImpl implements PurchaseValidator {

    private final TimeRestrictionValidatorService timeRestrictionValidatorService;
    private final HealthRestrictionValidatorService healthRestrictionValidatorService;
    private final BudgetValidatorService budgetValidatorService;

    public PurchaseValidatorImpl(
            TimeRestrictionValidatorService timeRestrictionValidatorService,
            HealthRestrictionValidatorService healthRestrictionValidatorService,
            BudgetValidatorService budgetValidatorService
    ) {
        this.timeRestrictionValidatorService = timeRestrictionValidatorService;
        this.healthRestrictionValidatorService = healthRestrictionValidatorService;
        this.budgetValidatorService = budgetValidatorService;
    }

    @Override
    public void validate(Purchase purchase) {
        timeRestrictionValidatorService.validatePurchase(
                purchase.getStudent(),
                purchase.getItems().keySet(),
                LocalDateTime.now()
        );

        healthRestrictionValidatorService.validatePurchase(
                purchase.getStudent(),
                purchase.getItems().keySet()
        );

        Map<UUID, BigDecimal> amountsByCategory = purchase.getItems().entrySet().stream()
                .collect(Collectors.groupingBy(
                        entry -> entry.getKey().getCategoria().getId(),
                        Collectors.mapping(
                                entry -> entry.getKey().getPrecio()
                                        .multiply(BigDecimal.valueOf(entry.getValue())),
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add)
                        )
                ));

        budgetValidatorService.validateBudget(
                purchase.getStudent().getId(),
                purchase.getTotalAmount(),
                amountsByCategory,
                LocalDateTime.now()
        );
    }
}
