package com.recred.backend.application;

import com.recred.backend.domain.models.TimeBadge;
import com.recred.backend.domain.models.Product;
import com.recred.backend.domain.services.TimeValidatorService;
import com.recred.backend.domain.services.RestrictionsValidatorService;
import com.recred.backend.domain.services.BudgetValidatorService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SchedulePickupTest {

    @Mock
    private TimeValidatorService timeValidator;

    @Mock
    private RestrictionsValidatorService healthValidator;

    @Mock
    private BudgetValidatorService budgetValidator;

    @InjectMocks
    private SchedulePickup schedulePickup;

    @Test
    void execute_EnsamblaFechaYHoraYLlamaAValidadores() {
        UUID studentId = UUID.randomUUID();
        Set<Product> cart = Set.of(new Product());

        LocalDate pickupDate = LocalDate.of(2026, 6, 1);
        TimeBadge pickupSlot = new TimeBadge();
        pickupSlot.setStartTime(LocalTime.of(10, 0));

        schedulePickup.execute(studentId, cart, pickupSlot, pickupDate);

        LocalDateTime expectedTime = LocalDateTime.of(2026, 6, 1, 10, 0);

        verify(timeValidator).validatePurchase(any(), eq(cart), eq(expectedTime));

        verify(healthValidator).validatePurchase(any(), eq(cart));

        verify(budgetValidator).validateBudget(
                eq(studentId),
                eq(java.math.BigDecimal.ZERO),
                eq(java.util.Collections.emptyMap()),
                eq(expectedTime)
        );
    }
}