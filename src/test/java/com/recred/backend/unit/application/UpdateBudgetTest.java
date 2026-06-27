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
        when(repository.findById(budgetId)).thenReturn(Optional.empty());

        UpdateBudgetRequest request = new UpdateBudgetRequest(
                budgetId,
                new BigDecimal("10000.00"),
                PeriodoPresupuesto.SEMANAL,
                LocalDate.now(),
                List.of()
        );

        assertThrows(RuntimeException.class, () -> {
            updateBudget.execute(request);
        });
    }

    @Test
    void execute_ActualizaDatosYCalculaPorcentajesCorrectamente() {
        when(repository.findById(budgetId)).thenReturn(Optional.of(presupuestoExistente));

        when(repository.save(any(PresupuestoAlumno.class))).thenAnswer(i -> i.getArguments()[0]);

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

        // Also assert on UpdateBudgetResponse DTO fields
        assertEquals(budgetId, response.id());
        assertEquals(new BigDecimal("10000.00"), response.montoLimiteGeneral());
        assertEquals(PeriodoPresupuesto.SEMANAL, response.periodo());
        assertNotNull(response.reglasCategoria());
        assertEquals(1, response.reglasCategoria().size());
        assertEquals(new BigDecimal("2000.00"), response.reglasCategoria().get(0).montoLimiteCalculado());
    }
}
