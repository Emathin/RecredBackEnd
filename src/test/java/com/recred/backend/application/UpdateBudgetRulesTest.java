package com.recred.backend.application;

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
    private PresupuestoAlumno presupuestoExistente;

    @BeforeEach
    void setUp() {
        budgetId = UUID.randomUUID();
        presupuestoExistente = new PresupuestoAlumno();
        presupuestoExistente.setId(budgetId);
        presupuestoExistente.setMontoLimiteGeneral(new BigDecimal("5000.00"));
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
        when(repository.save(any(PresupuestoAlumno.class))).thenAnswer(i -> i.getArguments()[0]);

        UpdateBudgetRulesCommand.CategoryRuleCommand ruleCommand =
                new UpdateBudgetRulesCommand.CategoryRuleCommand(UUID.randomUUID(), new BigDecimal("10.00"));
        UpdateBudgetRulesCommand command = new UpdateBudgetRulesCommand(List.of(ruleCommand));

        updateBudgetRules.execute(budgetId, command);

        ArgumentCaptor<PresupuestoAlumno> captor = ArgumentCaptor.forClass(PresupuestoAlumno.class);
        verify(repository).save(captor.capture());
        PresupuestoAlumno guardado = captor.getValue();

        assertEquals(1, guardado.getReglasCategoria().size());
        assertEquals(new BigDecimal("10.00"), guardado.getReglasCategoria().get(0).getPorcentajeLimite());

        assertEquals(new BigDecimal("500.00"), guardado.getReglasCategoria().get(0).getMontoLimiteCalculado());
    }

    @Test
    void execute_ListaVacia_LimpiaLasReglas() {
        when(repository.findById(budgetId)).thenReturn(Optional.of(presupuestoExistente));
        when(repository.save(any(PresupuestoAlumno.class))).thenAnswer(i -> i.getArguments()[0]);

        UpdateBudgetRulesCommand command = new UpdateBudgetRulesCommand(Collections.emptyList());

        updateBudgetRules.execute(budgetId, command);

        ArgumentCaptor<PresupuestoAlumno> captor = ArgumentCaptor.forClass(PresupuestoAlumno.class);
        verify(repository).save(captor.capture());

        assertTrue(captor.getValue().getReglasCategoria().isEmpty(), "La lista de reglas debería quedar vacía");
    }
}