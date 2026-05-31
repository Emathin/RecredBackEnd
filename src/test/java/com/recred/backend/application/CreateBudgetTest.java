package com.recred.backend.application;

import com.recred.backend.domain.models.PeriodBudget;
import com.recred.backend.domain.models.StudentBudget;
import com.recred.backend.domain.repositories.BudgetRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateBudgetTest {

    @Mock
    private BudgetRepository repository;

    @InjectMocks
    private CreateBudget createBudget;

    @Test
    void execute_CreaPresupuesto_CalculaReglas_YLoGuarda() {

        when(repository.save(any(StudentBudget.class))).thenAnswer(i -> i.getArguments()[0]);

        BudgetCommand command = mock(BudgetCommand.class);
        when(command.studentId()).thenReturn(UUID.randomUUID());
        when(command.limitAmount()).thenReturn(new BigDecimal("5000.00"));
        when(command.period()).thenReturn(PeriodBudget.MENSUAL);
        when(command.startDate()).thenReturn(LocalDate.now());

        var ruleMock = mock(BudgetCommand.CategoryRuleCommand.class);
        when(ruleMock.categoryId()).thenReturn(UUID.randomUUID());
        when(ruleMock.porcentajeLimite()).thenReturn(new BigDecimal("10.00"));
        when(command.categoryRules()).thenReturn(List.of(ruleMock));

        StudentBudget resultado = createBudget.execute(command);

        ArgumentCaptor<StudentBudget> captor = ArgumentCaptor.forClass(StudentBudget.class);
        verify(repository).save(captor.capture());
        StudentBudget guardado = captor.getValue();

        assertNotNull(guardado);
        assertTrue(guardado.isActive());
        assertEquals(new BigDecimal("5000.00"), guardado.getLimitAmount());
        assertEquals(PeriodBudget.MENSUAL, guardado.getPeriod());

        assertEquals(1, guardado.getCategoryRules().size());
        assertEquals(new BigDecimal("500.00"), guardado.getCategoryRules().get(0).getCalculatedLimitAmount());
    }
}