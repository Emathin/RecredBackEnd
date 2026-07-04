package com.recred.backend.unit.application;

import com.recred.backend.application.GetActiveBudgetByStudent;
import com.recred.backend.application.GetActiveBudgetByStudentRequest;
import com.recred.backend.application.GetActiveBudgetByStudentResponse;
import com.recred.backend.domain.models.CategoriaProducto;
import com.recred.backend.domain.models.PeriodoPresupuesto;
import com.recred.backend.domain.models.PresupuestoAlumno;
import com.recred.backend.domain.models.ReglaPresupuestoCategoria;
import com.recred.backend.domain.repositories.PresupuestoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetActiveBudgetByStudentTest {

    @Mock
    private PresupuestoRepository repository;

    @InjectMocks
    private GetActiveBudgetByStudent getActiveBudgetByStudent;

    @Test
    void execute_DelegaLaConsultaAlRepositorio() {
        UUID studentId = UUID.randomUUID();
        PresupuestoAlumno presupuesto = new PresupuestoAlumno();
        presupuesto.setId(UUID.randomUUID());
        presupuesto.setAlumnoId(studentId);
        presupuesto.setActivo(true);
        GetActiveBudgetByStudentRequest request = new GetActiveBudgetByStudentRequest(studentId);

        when(repository.findActivoByAlumnoId(studentId)).thenReturn(Optional.of(presupuesto));

        Optional<GetActiveBudgetByStudentResponse> resultado = getActiveBudgetByStudent.execute(request);

        assertTrue(resultado.isPresent());
        assertEquals(presupuesto.getId(), resultado.get().id());
        assertEquals(presupuesto.getAlumnoId(), resultado.get().alumnoId());
        verify(repository).findActivoByAlumnoId(studentId);
    }

    @Test
    void execute_CuandoHayReglas_MapeaCategoriasYLimites() {
        UUID studentId = UUID.randomUUID();
        UUID budgetId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        UUID ruleId = UUID.randomUUID();
        LocalDate startDate = LocalDate.of(2026, 7, 1);
        CategoriaProducto category = new CategoriaProducto();
        category.setId(categoryId);
        category.setDescripcion("Snacks");
        category.setActivo(false);
        ReglaPresupuestoCategoria categoryRule = new ReglaPresupuestoCategoria(
                ruleId,
                category,
                new BigDecimal("30.00"),
                new BigDecimal("300.00"),
                true
        );
        ReglaPresupuestoCategoria uncategorizedRule = new ReglaPresupuestoCategoria(
                UUID.randomUUID(),
                null,
                new BigDecimal("10.00"),
                new BigDecimal("100.00"),
                false
        );
        PresupuestoAlumno budget = new PresupuestoAlumno(
                budgetId,
                studentId,
                creatorId,
                new BigDecimal("1000.00"),
                PeriodoPresupuesto.MENSUAL,
                startDate,
                true,
                List.of(categoryRule, uncategorizedRule)
        );
        GetActiveBudgetByStudentRequest request = new GetActiveBudgetByStudentRequest(studentId);

        when(repository.findActivoByAlumnoId(studentId)).thenReturn(Optional.of(budget));

        Optional<GetActiveBudgetByStudentResponse> result = getActiveBudgetByStudent.execute(request);

        assertTrue(result.isPresent());
        GetActiveBudgetByStudentResponse response = result.orElseThrow();
        assertEquals(budgetId, response.id());
        assertEquals(creatorId, response.creadoPorUsuarioId());
        assertEquals(new BigDecimal("1000.00"), response.montoLimiteGeneral());
        assertEquals(PeriodoPresupuesto.MENSUAL, response.periodo());
        assertEquals(startDate, response.fechaInicio());
        assertEquals(new BigDecimal("1000.00"), response.montoLimite());
        assertEquals(2, response.reglasCategoria().size());

        GetActiveBudgetByStudentResponse.ReglaPresupuestoCategoriaResponse mappedRule =
                response.reglasCategoria().get(0);
        assertEquals(ruleId, mappedRule.id());
        assertEquals(new BigDecimal("30.00"), mappedRule.porcentajeLimite());
        assertEquals(new BigDecimal("300.00"), mappedRule.montoLimiteCalculado());
        assertEquals(categoryId, mappedRule.categoria().id());
        assertEquals("Snacks", mappedRule.categoria().descripcion());
        assertEquals(false, mappedRule.categoria().activo());

        assertNull(response.reglasCategoria().get(1).categoria());
        assertEquals(false, response.reglasCategoria().get(1).activo());
    }

    @Test
    void execute_CuandoReglasSonNull_RetornaReglasNull() {
        UUID studentId = UUID.randomUUID();
        PresupuestoAlumno budget = new PresupuestoAlumno();
        budget.setId(UUID.randomUUID());
        budget.setAlumnoId(studentId);
        budget.setReglasCategoria(null);
        GetActiveBudgetByStudentRequest request = new GetActiveBudgetByStudentRequest(studentId);

        when(repository.findActivoByAlumnoId(studentId)).thenReturn(Optional.of(budget));

        Optional<GetActiveBudgetByStudentResponse> result = getActiveBudgetByStudent.execute(request);

        assertTrue(result.isPresent());
        assertNull(result.orElseThrow().reglasCategoria());
    }

    @Test
    void execute_CuandoNoHayPresupuestoActivo_RetornaOptionalVacio() {
        UUID studentId = UUID.randomUUID();
        GetActiveBudgetByStudentRequest request = new GetActiveBudgetByStudentRequest(studentId);

        when(repository.findActivoByAlumnoId(studentId)).thenReturn(Optional.empty());

        Optional<GetActiveBudgetByStudentResponse> resultado = getActiveBudgetByStudent.execute(request);

        assertFalse(resultado.isPresent());
        verify(repository).findActivoByAlumnoId(studentId);
    }
}
