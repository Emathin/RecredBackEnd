package com.recred.backend.application;

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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateBudgetRulesTest {

    @Mock
    private BudgetRepository repository;

    @InjectMocks
    private UpdateBudgetRules updateBudgetRules;

    private UUID budgetId;
    private StudentBudget presupuestoExistente;

    @BeforeEach
    void setUp() {
        budgetId = UUID.randomUUID();
        presupuestoExistente = new StudentBudget();
        presupuestoExistente.setId(budgetId);
        presupuestoExistente.setLimitAmount(new BigDecimal("5000.00"));
    }

    @Test
    void execute_PresupuestoNoExiste_LanzaExcepcion() {
        when(repository.findById(budgetId)).thenReturn(Optional.empty());
        UpdateBudgetRulesCommand command = new UpdateBudgetRulesCommand(Collections.emptyList());

        assertThrows(RuntimeException.class, () -> updateBudgetRules.execute(budgetId, command));
    }

    @Test
    void execute_ActualizaReglasYGuarda() {
        when(repository.findById(budgetId)).thenReturn(Optional.of(presupuestoExistente));
        when(repository.save(any(StudentBudget.class))).thenAnswer(i -> i.getArguments()[0]);

        UpdateBudgetRulesCommand.CategoryRuleCommand ruleCommand =
                new UpdateBudgetRulesCommand.CategoryRuleCommand(UUID.randomUUID(), new BigDecimal("10.00"));
        UpdateBudgetRulesCommand command = new UpdateBudgetRulesCommand(List.of(ruleCommand));

        updateBudgetRules.execute(budgetId, command);

        ArgumentCaptor<StudentBudget> captor = ArgumentCaptor.forClass(StudentBudget.class);
        verify(repository).save(captor.capture());
        StudentBudget guardado = captor.getValue();

        assertEquals(1, guardado.getCategoryRules().size());
        assertEquals(new BigDecimal("10.00"), guardado.getCategoryRules().get(0).getLimitAverage());

        assertEquals(new BigDecimal("500.00"), guardado.getCategoryRules().get(0).getCalculatedLimitAmount());
    }

    @Test
    void execute_ListaVacia_LimpiaLasReglas() {
        when(repository.findById(budgetId)).thenReturn(Optional.of(presupuestoExistente));
        when(repository.save(any(StudentBudget.class))).thenAnswer(i -> i.getArguments()[0]);

        UpdateBudgetRulesCommand command = new UpdateBudgetRulesCommand(Collections.emptyList());

        updateBudgetRules.execute(budgetId, command);

        ArgumentCaptor<StudentBudget> captor = ArgumentCaptor.forClass(StudentBudget.class);
        verify(repository).save(captor.capture());

        assertTrue(captor.getValue().getCategoryRules().isEmpty(), "La lista de reglas debería quedar vacía");
    }
}