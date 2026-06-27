package com.recred.backend.unit.application;

import com.recred.backend.application.UpdateBudgetRules;
import com.recred.backend.application.UpdateBudgetRulesRequest;
import com.recred.backend.application.UpdateBudgetRulesResponse;
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
        UpdateBudgetRulesRequest request = new UpdateBudgetRulesRequest(budgetId, Collections.emptyList());
 
        assertThrows(RuntimeException.class, () -> updateBudgetRules.execute(request));
    }
 
    @Test
    void execute_ActualizaReglasYGuarda() {
        when(repository.findById(budgetId)).thenReturn(Optional.of(presupuestoExistente));
        when(repository.save(any(PresupuestoAlumno.class))).thenAnswer(i -> i.getArguments()[0]);
 
        UUID categoryId = UUID.randomUUID();
        UpdateBudgetRulesRequest.CategoryRuleRequest rule =
                new UpdateBudgetRulesRequest.CategoryRuleRequest(categoryId, new BigDecimal("10.00"));
        UpdateBudgetRulesRequest request = new UpdateBudgetRulesRequest(budgetId, List.of(rule));
 
        UpdateBudgetRulesResponse response = updateBudgetRules.execute(request);
 
        ArgumentCaptor<PresupuestoAlumno> captor = ArgumentCaptor.forClass(PresupuestoAlumno.class);
        verify(repository).save(captor.capture());
        PresupuestoAlumno guardado = captor.getValue();
 
        assertEquals(1, guardado.getReglasCategoria().size());
        assertEquals(new BigDecimal("10.00"), guardado.getReglasCategoria().get(0).getPorcentajeLimite());
 
        assertEquals(new BigDecimal("500.00"), guardado.getReglasCategoria().get(0).getMontoLimiteCalculado());

        // Assert response values
        assertEquals(budgetId, response.id());
        assertEquals(1, response.reglasCategoria().size());
        assertEquals(new BigDecimal("10.00"), response.reglasCategoria().get(0).porcentajeLimite());
        assertEquals(new BigDecimal("500.00"), response.reglasCategoria().get(0).montoLimiteCalculado());
    }
 
    @Test
    void execute_ListaVacia_LimpiaLasReglas() {
        when(repository.findById(budgetId)).thenReturn(Optional.of(presupuestoExistente));
        when(repository.save(any(PresupuestoAlumno.class))).thenAnswer(i -> i.getArguments()[0]);
 
        UpdateBudgetRulesRequest request = new UpdateBudgetRulesRequest(budgetId, Collections.emptyList());
 
        UpdateBudgetRulesResponse response = updateBudgetRules.execute(request);
 
        ArgumentCaptor<PresupuestoAlumno> captor = ArgumentCaptor.forClass(PresupuestoAlumno.class);
        verify(repository).save(captor.capture());
 
        assertTrue(captor.getValue().getReglasCategoria().isEmpty(), "La lista de reglas debería quedar vacía");
        assertTrue(response.reglasCategoria().isEmpty());
    }
}
