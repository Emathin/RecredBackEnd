package com.recred.backend.unit.application;

import com.recred.backend.application.UpdateBudgetRules;
import com.recred.backend.application.UpdateBudgetRulesCommand;
import com.recred.backend.domain.models.PresupuestoAlumno;
import com.recred.backend.domain.repositories.PresupuestoRepository;
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
    private PresupuestoRepository repository;

    @InjectMocks
    private UpdateBudgetRules updateBudgetRules;

    private UUID budgetId;
    private PresupuestoAlumno existingBudget;

    @BeforeEach
    void setUp() {
        budgetId = UUID.randomUUID();
        existingBudget = new PresupuestoAlumno();
        existingBudget.setId(budgetId);
        existingBudget.setMontoLimiteGeneral(new BigDecimal("5000.00"));
    }

    @Test
    void executeWhenBudgetDoesNotExistShouldThrowException() {
        when(repository.findById(budgetId)).thenReturn(Optional.empty());
        UpdateBudgetRulesCommand command = new UpdateBudgetRulesCommand(Collections.emptyList());

        assertThrows(RuntimeException.class, () -> updateBudgetRules.execute(budgetId, command));
    }

    @Test
    void executeShouldUpdateRulesAndSave() {
        when(repository.findById(budgetId)).thenReturn(Optional.of(existingBudget));
        when(repository.save(any(PresupuestoAlumno.class))).thenAnswer(i -> i.getArguments()[0]);

        UpdateBudgetRulesCommand.CategoryRuleCommand ruleCommand =
                new UpdateBudgetRulesCommand.CategoryRuleCommand(UUID.randomUUID(), new BigDecimal("10.00"));
        UpdateBudgetRulesCommand command = new UpdateBudgetRulesCommand(List.of(ruleCommand));

        updateBudgetRules.execute(budgetId, command);

        ArgumentCaptor<PresupuestoAlumno> captor = ArgumentCaptor.forClass(PresupuestoAlumno.class);
        verify(repository).save(captor.capture());
        PresupuestoAlumno saved = captor.getValue();

        assertEquals(1, saved.getReglasCategoria().size());
        assertEquals(new BigDecimal("10.00"), saved.getReglasCategoria().get(0).getPorcentajeLimite());

        assertEquals(new BigDecimal("500.00"), saved.getReglasCategoria().get(0).getMontoLimiteCalculado());
    }

    @Test
    void executeWithEmptyListShouldClearRules() {
        when(repository.findById(budgetId)).thenReturn(Optional.of(existingBudget));
        when(repository.save(any(PresupuestoAlumno.class))).thenAnswer(i -> i.getArguments()[0]);

        UpdateBudgetRulesCommand command = new UpdateBudgetRulesCommand(Collections.emptyList());

        updateBudgetRules.execute(budgetId, command);

        ArgumentCaptor<PresupuestoAlumno> captor = ArgumentCaptor.forClass(PresupuestoAlumno.class);
        verify(repository).save(captor.capture());

        assertTrue(captor.getValue().getReglasCategoria().isEmpty(), "The rule list should be empty");
    }
}
