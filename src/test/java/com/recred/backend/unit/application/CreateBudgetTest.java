package com.recred.backend.unit.application;

import com.recred.backend.application.CreateBudget;
import com.recred.backend.application.CreateBudgetRequest;
import com.recred.backend.application.CreateBudgetResponse;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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

        when(repository.save(any(PresupuestoAlumno.class))).thenAnswer(i -> {
            PresupuestoAlumno p = (PresupuestoAlumno) i.getArguments()[0];
            p.setId(UUID.randomUUID());
            if (p.getReglasCategoria() != null) {
                p.getReglasCategoria().forEach(r -> r.setId(UUID.randomUUID()));
            }
            return p;
        });

        UUID studentId = UUID.randomUUID();
        UUID creadorId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();

        CreateBudgetRequest request = new CreateBudgetRequest(
                studentId,
                creadorId,
                new BigDecimal("5000.00"),
                PeriodoPresupuesto.MENSUAL,
                LocalDate.now(),
                List.of(new CreateBudgetRequest.CategoryRuleRequest(categoryId, new BigDecimal("10.00")))
        );

        CreateBudgetResponse resultado = createBudget.execute(request);

        ArgumentCaptor<PresupuestoAlumno> captor = ArgumentCaptor.forClass(PresupuestoAlumno.class);
        verify(repository).save(captor.capture());
        PresupuestoAlumno guardado = captor.getValue();

        assertNotNull(guardado);
        assertTrue(guardado.isActivo());
        assertEquals(new BigDecimal("5000.00"), guardado.getMontoLimiteGeneral());
        assertEquals(PeriodoPresupuesto.MENSUAL, guardado.getPeriodo());

        assertEquals(1, guardado.getReglasCategoria().size());
        assertEquals(new BigDecimal("500.00"), guardado.getReglasCategoria().get(0).getMontoLimiteCalculado());

        assertNotNull(resultado);
        assertEquals(guardado.getId(), resultado.id());
        assertEquals(studentId, resultado.alumnoId());
        assertEquals(creadorId, resultado.creadoPorUsuarioId());
        assertEquals(new BigDecimal("5000.00"), resultado.montoLimiteGeneral());
        assertEquals(PeriodoPresupuesto.MENSUAL, resultado.periodo());
        assertTrue(resultado.activo());
        assertEquals(1, resultado.reglasCategoria().size());
        assertEquals(new BigDecimal("500.00"), resultado.reglasCategoria().get(0).montoLimiteCalculado());
    }

    @Test
    void execute_CuandoSumaPorcentajesSupera100_LanzaIllegalArgumentException() {
        UUID studentId = UUID.randomUUID();
        UUID creadorId = UUID.randomUUID();
        UUID category1 = UUID.randomUUID();
        UUID category2 = UUID.randomUUID();

        CreateBudgetRequest request = new CreateBudgetRequest(
                studentId,
                creadorId,
                new BigDecimal("5000.00"),
                PeriodoPresupuesto.MENSUAL,
                LocalDate.now(),
                List.of(
                        new CreateBudgetRequest.CategoryRuleRequest(category1, new BigDecimal("60.00")),
                        new CreateBudgetRequest.CategoryRuleRequest(category2, new BigDecimal("50.00"))
                )
        );

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                createBudget.execute(request)
        );
        assertTrue(exception.getMessage().contains("La suma de los porcentajes no puede superar el 100%"));
    }
}
