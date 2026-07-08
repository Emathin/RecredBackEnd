package com.recred.backend.unit.application;

import com.recred.backend.application.UpdateBudget;
import com.recred.backend.application.UpdateBudgetRequest;
import com.recred.backend.application.UpdateBudgetResponse;
import com.recred.backend.domain.models.PeriodoPresupuesto;
import com.recred.backend.domain.models.PresupuestoAlumno;
import com.recred.backend.domain.repositories.PresupuestoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpdateBudgetTest {

    @Mock
    private PresupuestoRepository repository;

    @InjectMocks
    private UpdateBudget updateBudget;

    private UUID budgetId;
    private PresupuestoAlumno presupuestoExistente;

    @BeforeEach
    void setUp() {
        budgetId = UUID.randomUUID();
        presupuestoExistente = new PresupuestoAlumno();
        presupuestoExistente.setId(budgetId);
    }

    @Test
    void execute_PresupuestoNoExiste_LanzaExcepcion() {
        UpdateBudgetRequest request = new UpdateBudgetRequest(
                budgetId,
                new BigDecimal("10000.00"),
                PeriodoPresupuesto.SEMANAL,
                LocalDate.now(),
                List.of()
        );

        when(repository.findById(budgetId)).thenReturn(Optional.empty());

        Executable execution = () -> updateBudget.execute(request);

        assertThrows(RuntimeException.class, execution);
        verify(repository, never()).save(any());
    }

    @Test
    void execute_ActualizaDatosYCalculaPorcentajesCorrectamente() {
        UUID categoryId = UUID.randomUUID();
        UpdateBudgetRequest.CategoryRuleRequest rule = new UpdateBudgetRequest.CategoryRuleRequest(
                categoryId,
                new BigDecimal("20.00")
        );

        UpdateBudgetRequest request = new UpdateBudgetRequest(
                budgetId,
                new BigDecimal("10000.00"),
                PeriodoPresupuesto.SEMANAL,
                LocalDate.now(),
                List.of(rule)
        );

        when(repository.findById(budgetId)).thenReturn(Optional.of(presupuestoExistente));
        when(repository.save(any(PresupuestoAlumno.class))).thenAnswer(i -> i.getArguments()[0]);

        UpdateBudgetResponse response = updateBudget.execute(request);

        ArgumentCaptor<PresupuestoAlumno> captor = ArgumentCaptor.forClass(PresupuestoAlumno.class);
        verify(repository).save(captor.capture());
        PresupuestoAlumno guardado = captor.getValue();

        assertEquals(new BigDecimal("10000.00"), guardado.getMontoLimiteGeneral());
        assertEquals(PeriodoPresupuesto.SEMANAL, guardado.getPeriodo());

        assertNotNull(guardado.getReglasCategoria());
        assertEquals(1, guardado.getReglasCategoria().size());

        BigDecimal montoCalculado = guardado.getReglasCategoria().get(0).getMontoLimiteCalculado();
        assertEquals(new BigDecimal("2000.00"), montoCalculado);

        assertEquals(budgetId, response.id());
        assertEquals(new BigDecimal("10000.00"), response.montoLimiteGeneral());
        assertEquals(PeriodoPresupuesto.SEMANAL, response.periodo());
        assertNotNull(response.reglasCategoria());
        assertEquals(1, response.reglasCategoria().size());
        assertEquals(new BigDecimal("2000.00"), response.reglasCategoria().get(0).montoLimiteCalculado());
    }

    @Test
    void execute_CuandoSumaPorcentajesSupera100_LanzaIllegalArgumentException() {
        UpdateBudgetRequest.CategoryRuleRequest rule1 = new UpdateBudgetRequest.CategoryRuleRequest(
                UUID.randomUUID(),
                new BigDecimal("60.00")
        );
        UpdateBudgetRequest.CategoryRuleRequest rule2 = new UpdateBudgetRequest.CategoryRuleRequest(
                UUID.randomUUID(),
                new BigDecimal("50.00")
        );

        UpdateBudgetRequest request = new UpdateBudgetRequest(
                budgetId,
                new BigDecimal("10000.00"),
                PeriodoPresupuesto.SEMANAL,
                LocalDate.now(),
                List.of(rule1, rule2)
        );

        when(repository.findById(budgetId)).thenReturn(Optional.of(presupuestoExistente));

        Executable execution = () -> updateBudget.execute(request);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, execution);
        assertTrue(exception.getMessage().contains("La suma de los porcentajes no puede superar el 100%"));
        verify(repository, never()).save(any());
    }
}
