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

    private static final UUID STUDENT_ID = UUID.randomUUID();
    private static final UUID BUDGET_ID = UUID.randomUUID();
    private static final UUID CREATOR_ID = UUID.randomUUID();
    private static final UUID CATEGORY_ID = UUID.randomUUID();
    private static final UUID RULE_ID = UUID.randomUUID();
    private static final LocalDate START_DATE = LocalDate.of(2026, 7, 1);
    
    private static final BigDecimal LIMIT = new BigDecimal("1000.00");
    private static final BigDecimal RULE_PERCENTAGE = new BigDecimal("30.00");
    private static final BigDecimal RULE_LIMIT = new BigDecimal("300.00");

    @Mock
    private PresupuestoRepository repository;

    @InjectMocks
    private GetActiveBudgetByStudent getActiveBudgetByStudent;

    @Test
    void delegatesQueryToRepository() {
        PresupuestoAlumno budget = buildSimpleBudget();
        givenActiveBudgetFound(budget);

        Optional<GetActiveBudgetByStudentResponse> result = whenExecute();

        thenResultIsPresent(result, budget);
        thenRepositoryWasQueriedByStudentId();
    }

    @Test
    void mapsCategoriesAndLimitsWhenRulesExist() {
        PresupuestoAlumno budget = buildBudgetWithRules();
        givenActiveBudgetFound(budget);

        Optional<GetActiveBudgetByStudentResponse> result = whenExecute();

        thenMappedRulesAreCorrect(result.orElseThrow());
    }

    @Test
    void returnsNullRulesWhenRulesAreNull() {
        PresupuestoAlumno budget = buildBudgetWithNullRules();
        givenActiveBudgetFound(budget);

        Optional<GetActiveBudgetByStudentResponse> result = whenExecute();

        thenRulesAreNull(result);
    }

    @Test
    void returnsEmptyOptionalWhenNoActiveBudgetExists() {
        givenNoActiveBudgetFound();

        Optional<GetActiveBudgetByStudentResponse> result = whenExecute();

        thenResultIsEmpty(result);
        thenRepositoryWasQueriedByStudentId();
    }

    private void givenActiveBudgetFound(PresupuestoAlumno budget) {
        when(repository.findActivoByAlumnoId(STUDENT_ID)).thenReturn(Optional.of(budget));
    }

    private void givenNoActiveBudgetFound() {
        when(repository.findActivoByAlumnoId(STUDENT_ID)).thenReturn(Optional.empty());
    }

    private Optional<GetActiveBudgetByStudentResponse> whenExecute() {
        return getActiveBudgetByStudent.execute(new GetActiveBudgetByStudentRequest(STUDENT_ID));
    }

    private void thenResultIsPresent(Optional<GetActiveBudgetByStudentResponse> result, PresupuestoAlumno budget) {
        assertTrue(result.isPresent());
        assertEquals(budget.getId(), result.get().id());
        assertEquals(budget.getAlumnoId(), result.get().alumnoId());
    }

    private void thenMappedRulesAreCorrect(GetActiveBudgetByStudentResponse response) {
        assertEquals(BUDGET_ID, response.id());
        assertEquals(CREATOR_ID, response.creadoPorUsuarioId());
        assertEquals(LIMIT, response.montoLimiteGeneral());
        assertEquals(PeriodoPresupuesto.MENSUAL, response.periodo());
        assertEquals(START_DATE, response.fechaInicio());
        assertEquals(LIMIT, response.montoLimite());
        assertEquals(2, response.reglasCategoria().size());

        GetActiveBudgetByStudentResponse.ReglaPresupuestoCategoriaResponse mappedRule = response.reglasCategoria().get(0);
        assertEquals(RULE_ID, mappedRule.id());
        assertEquals(RULE_PERCENTAGE, mappedRule.porcentajeLimite());
        assertEquals(RULE_LIMIT, mappedRule.montoLimiteCalculado());
        assertEquals(CATEGORY_ID, mappedRule.categoria().id());
        assertEquals("Snacks", mappedRule.categoria().descripcion());
        assertFalse(mappedRule.categoria().activo());

        assertNull(response.reglasCategoria().get(1).categoria());
        assertFalse(response.reglasCategoria().get(1).activo());
    }

    private PresupuestoAlumno buildBudgetWithRules() {
        CategoriaProducto category = new CategoriaProducto();
        category.setId(CATEGORY_ID);
        category.setDescripcion("Snacks");
        category.setActivo(false);

        ReglaPresupuestoCategoria categoryRule = new ReglaPresupuestoCategoria(
                RULE_ID, category, RULE_PERCENTAGE, RULE_LIMIT, true
        );

        ReglaPresupuestoCategoria uncategorizedRule = new ReglaPresupuestoCategoria(
                UUID.randomUUID(), null, new BigDecimal("10.00"), new BigDecimal("100.00"), false
        );

        return new PresupuestoAlumno(
                BUDGET_ID, STUDENT_ID, CREATOR_ID, LIMIT,
                PeriodoPresupuesto.MENSUAL, START_DATE, true,
                List.of(categoryRule, uncategorizedRule)
        );
    }

    private PresupuestoAlumno buildSimpleBudget() {
        PresupuestoAlumno budget = new PresupuestoAlumno();
        budget.setId(BUDGET_ID);
        budget.setAlumnoId(STUDENT_ID);
        budget.setActivo(true);
        return budget;
    }

    private PresupuestoAlumno buildBudgetWithNullRules() {
        PresupuestoAlumno budget = new PresupuestoAlumno();
        budget.setId(BUDGET_ID);
        budget.setAlumnoId(STUDENT_ID);
        budget.setReglasCategoria(null);
        return budget;
    }

    private void thenRepositoryWasQueriedByStudentId() {
        verify(repository).findActivoByAlumnoId(STUDENT_ID);
    }

    private void thenRulesAreNull(Optional<GetActiveBudgetByStudentResponse> result) {
        assertTrue(result.isPresent());
        assertNull(result.get().reglasCategoria());
    }

    private void thenResultIsEmpty(Optional<GetActiveBudgetByStudentResponse> result) {
        assertFalse(result.isPresent());
    }
}
