package com.recred.backend.application;

import com.recred.backend.domain.models.PeriodBudget;
import com.recred.backend.domain.models.StudentBudget;
import com.recred.backend.domain.repositories.BudgetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateBudgetTest {

    @Mock
    private BudgetRepository repository;

    @InjectMocks
    private UpdateBudget updateBudget;

    private UUID budgetId;
    private StudentBudget presupuestoExistente;

    @BeforeEach
    void setUp() {
        budgetId = UUID.randomUUID();
        presupuestoExistente = new StudentBudget();
        presupuestoExistente.setId(budgetId);
    }

    @Test
    void execute_PresupuestoNoExiste_LanzaExcepcion() {
        when(repository.findById(budgetId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            updateBudget.execute(budgetId, mock(BudgetCommand.class));
        });
    }

    @Test
    void execute_ActualizaDatosYCalculaPorcentajesCorrectamente() {
        when(repository.findById(budgetId)).thenReturn(Optional.of(presupuestoExistente));

        when(repository.save(any(StudentBudget.class))).thenAnswer(i -> i.getArguments()[0]);

        BudgetCommand command = mock(BudgetCommand.class);
        when(command.limitAmount()).thenReturn(new BigDecimal("10000.00"));
        when(command.period()).thenReturn(PeriodBudget.SEMANAL);
        when(command.startDate()).thenReturn(LocalDate.now());

        var ruleMock = mock(BudgetCommand.CategoryRuleCommand.class);
        when(ruleMock.categoryId()).thenReturn(UUID.randomUUID());
        when(ruleMock.porcentajeLimite()).thenReturn(new BigDecimal("20.00"));
        when(command.categoryRules()).thenReturn(List.of(ruleMock));

        StudentBudget resultado = updateBudget.execute(budgetId, command);

        ArgumentCaptor<StudentBudget> captor = ArgumentCaptor.forClass(StudentBudget.class);
        verify(repository).save(captor.capture());
        StudentBudget guardado = captor.getValue();

        assertEquals(new BigDecimal("10000.00"), guardado.getLimitAmount());
        assertEquals(PeriodBudget.SEMANAL, guardado.getPeriod());

        assertNotNull(guardado.getCategoryRules());
        assertEquals(1, guardado.getCategoryRules().size());

        BigDecimal montoCalculado = guardado.getCategoryRules().get(0).getCalculatedLimitAmount();
        assertEquals(new BigDecimal("2000.00"), montoCalculado);
    }
}