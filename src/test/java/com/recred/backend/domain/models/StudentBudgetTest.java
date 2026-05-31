package com.recred.backend.domain.models;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StudentBudgetTest {

    @Test
    void isCurrent_FechaAnteriorAlInicio_RetornaFalse() {
        StudentBudget presupuesto = new StudentBudget();

        presupuesto.setStartDate(LocalDate.now().plusDays(1));

        boolean resultado = presupuesto.isCurrent(LocalDate.now());

        assertFalse(resultado, "No debería ser vigente si la fecha de consulta es anterior al inicio");
    }

    @Test
    void isCurrent_FechaIgualAlInicio_RetornaTrue() {
        StudentBudget presupuesto = new StudentBudget();

        presupuesto.setStartDate(LocalDate.now());

        boolean resultado = presupuesto.isCurrent(LocalDate.now());

        assertTrue(resultado, "Debería ser vigente si la fecha coincide con el inicio");
    }

    @Test
    void isCurrent_FechaPosteriorAlInicio_RetornaTrue() {
        StudentBudget presupuesto = new StudentBudget();

        presupuesto.setStartDate(LocalDate.now().minusDays(1));

        boolean resultado = presupuesto.isCurrent(LocalDate.now());

        assertTrue(resultado, "Debería ser vigente si la fecha es posterior al inicio");
    }

    @Test
    void validatePercentage_SumaMenorOIgualA100_NoHaceNada() {
        StudentBudget presupuesto = new StudentBudget();

        BudgetCategoryRule regla1 = new BudgetCategoryRule();
        regla1.setLimitAverage(new BigDecimal("50.00"));
        regla1.setActive(true);

        BudgetCategoryRule regla2 = new BudgetCategoryRule();
        regla2.setLimitAverage(new BigDecimal("50.00"));
        regla2.setActive(true);

        presupuesto.setCategoryRules(List.of(regla1, regla2));

        assertDoesNotThrow(presupuesto::validatePercentage);
    }

    @Test
    void validatePercentage_SumaMayorA100_LanzaExcepcion() {
        StudentBudget presupuesto = new StudentBudget();

        BudgetCategoryRule regla1 = new BudgetCategoryRule();
        regla1.setLimitAverage(new BigDecimal("60.00"));
        regla1.setActive(true);

        BudgetCategoryRule regla2 = new BudgetCategoryRule();
        regla2.setLimitAverage(new BigDecimal("50.00"));
        regla2.setActive(true);

        presupuesto.setCategoryRules(List.of(regla1, regla2));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, presupuesto::validatePercentage);
        assertTrue(ex.getMessage().contains("no puede superar el 100%"));
    }

    @Test
    void recalculateRulesBudgets_CalculaElPorcentajeCorrectamente() {
        StudentBudget presupuesto = new StudentBudget();
        presupuesto.setLimitAmount(new BigDecimal("10000.00"));

        BudgetCategoryRule regla = new BudgetCategoryRule();
        regla.setLimitAverage(new BigDecimal("20.00"));
        regla.setActive(true);

        presupuesto.setCategoryRules(List.of(regla));

        presupuesto.recalculateRulesBudgets();

        assertEquals(new BigDecimal("2000.00"), presupuesto.getCategoryRules().get(0).getCalculatedLimitAmount());
    }
}