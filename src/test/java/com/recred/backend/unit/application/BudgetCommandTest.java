package com.recred.backend.unit.application;

import com.recred.backend.application.*;

import com.recred.backend.domain.models.PeriodoPresupuesto;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BudgetCommandTest {

    @Test
    void categoryRuleCommandStoresValues() {
        UUID studentId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        BudgetCommand.CategoryRuleCommand rule =
                new BudgetCommand.CategoryRuleCommand(categoryId, new BigDecimal("25.00"));
        BudgetCommand command = new BudgetCommand(
                studentId,
                creatorId,
                new BigDecimal("1000.00"),
                PeriodoPresupuesto.MENSUAL,
                LocalDate.of(2026, 6, 1),
                List.of(rule)
        );

        assertEquals(studentId, command.studentId());
        assertEquals(creatorId, command.creadorId());
        assertEquals(categoryId, command.categoryRules().get(0).categoryId());
        assertEquals(new BigDecimal("25.00"),
                command.categoryRules().get(0).porcentajeLimite());
    }
}
