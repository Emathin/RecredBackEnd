package com.recred.backend.unit.application;

import com.recred.backend.application.BudgetCommand;
import com.recred.backend.application.CreateBudget;
import com.recred.backend.domain.models.PeriodoPresupuesto;
import com.recred.backend.domain.models.PresupuestoAlumno;
import com.recred.backend.domain.repositories.PresupuestoRepository;
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
    private PresupuestoRepository repository;

    @InjectMocks
    private CreateBudget createBudget;

    @Test
    void execute_CreaPresupuesto_CalculaReglas_YLoGuarda() {

        when(repository.save(any(PresupuestoAlumno.class))).thenAnswer(i -> i.getArguments()[0]);

        BudgetCommand command = mock(BudgetCommand.class);
        when(command.studentId()).thenReturn(UUID.randomUUID());
        when(command.limitAmount()).thenReturn(new BigDecimal("5000.00"));
        when(command.period()).thenReturn(PeriodoPresupuesto.MENSUAL);
        when(command.startDate()).thenReturn(LocalDate.now());

        var ruleMock = mock(BudgetCommand.CategoryRuleCommand.class);
        when(ruleMock.categoryId()).thenReturn(UUID.randomUUID());
        when(ruleMock.porcentajeLimite()).thenReturn(new BigDecimal("10.00"));
        when(command.categoryRules()).thenReturn(List.of(ruleMock));

        PresupuestoAlumno resultado = createBudget.execute(command);

        ArgumentCaptor<PresupuestoAlumno> captor = ArgumentCaptor.forClass(PresupuestoAlumno.class);
        verify(repository).save(captor.capture());
        PresupuestoAlumno guardado = captor.getValue();

        assertNotNull(guardado);
        assertTrue(guardado.isActivo());
        assertEquals(new BigDecimal("5000.00"), guardado.getMontoLimiteGeneral());
        assertEquals(PeriodoPresupuesto.MENSUAL, guardado.getPeriodo());

        assertEquals(1, guardado.getReglasCategoria().size());
        assertEquals(new BigDecimal("500.00"), guardado.getReglasCategoria().get(0).getMontoLimiteCalculado());
    }
}
