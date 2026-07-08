package com.recred.backend.unit.application;

import com.recred.backend.application.CreateBudget;
import com.recred.backend.application.CreateBudgetRequest;
import com.recred.backend.application.CreateBudgetResponse;
import com.recred.backend.domain.models.PeriodoPresupuesto;
import com.recred.backend.domain.models.PresupuestoAlumno;
import com.recred.backend.domain.repositories.PresupuestoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateBudgetTest {

    private static final BigDecimal LIMIT_AMOUNT = new BigDecimal("5000.00");
    private static final BigDecimal TEN_PERCENT = new BigDecimal("10.00");
    private static final BigDecimal SIXTY_PERCENT = new BigDecimal("60.00");
    private static final BigDecimal FIFTY_PERCENT = new BigDecimal("50.00");
    private static final BigDecimal CALCULATED_CATEGORY_LIMIT = new BigDecimal("500.00");
    private static final LocalDate START_DATE = LocalDate.of(2026, 7, 6);

    @Mock
    private PresupuestoRepository repository;

    @Captor
    private ArgumentCaptor<PresupuestoAlumno> budgetCaptor;

    private CreateBudget createBudget;
    private CreateBudgetRequest createBudgetRequest;
    private CreateBudgetResponse createBudgetResponse;
    private Throwable thrownException;
    private UUID studentId;
    private UUID creatorId;
    private UUID categoryId;

    @BeforeEach
    void setUp() {
        createBudget = new CreateBudget(repository);
    }

    @Test
    void givenBudgetWithCategoryRule_whenCreatingBudget_thenCalculatesRulesAndSavesBudget() {
        givenBudgetWithCategoryRule();
        givenBudgetIsSaved();

        whenCreateBudget();

        thenBudgetWasSavedWithCalculatedRule();
        thenResponseContainsCreatedBudget();
    }

    @Test
    void givenBudgetWithoutCategoryRules_whenCreatingBudget_thenSavesBudgetWithoutRules() {
        givenBudgetWithoutCategoryRules();
        givenBudgetIsSaved();

        whenCreateBudget();

        thenBudgetWasSavedWithoutRules();
        thenResponseContainsBudgetWithoutRules();
    }

    @Test
    void givenCategoryPercentagesOverOneHundred_whenCreatingBudget_thenThrowsException() {
        givenBudgetWithCategoryPercentagesOverOneHundred();

        whenCreateBudgetFails(IllegalArgumentException.class);

        thenErrorMessageContains("La suma de los porcentajes no puede superar el 100%");
        thenBudgetWasNotSaved();
    }

    private void givenBudgetWithCategoryRule() {
        studentId = randomUUID();
        creatorId = randomUUID();
        categoryId = randomUUID();
        createBudgetRequest = new CreateBudgetRequest(
                studentId,
                creatorId,
                LIMIT_AMOUNT,
                PeriodoPresupuesto.MENSUAL,
                START_DATE,
                List.of(new CreateBudgetRequest.CategoryRuleRequest(categoryId, TEN_PERCENT))
        );
    }

    private void givenBudgetWithoutCategoryRules() {
        studentId = randomUUID();
        creatorId = randomUUID();
        createBudgetRequest = new CreateBudgetRequest(
                studentId,
                creatorId,
                LIMIT_AMOUNT,
                PeriodoPresupuesto.MENSUAL,
                START_DATE,
                null
        );
    }

    private void givenBudgetWithCategoryPercentagesOverOneHundred() {
        studentId = randomUUID();
        creatorId = randomUUID();
        createBudgetRequest = new CreateBudgetRequest(
                studentId,
                creatorId,
                LIMIT_AMOUNT,
                PeriodoPresupuesto.MENSUAL,
                START_DATE,
                List.of(
                        new CreateBudgetRequest.CategoryRuleRequest(randomUUID(), SIXTY_PERCENT),
                        new CreateBudgetRequest.CategoryRuleRequest(randomUUID(), FIFTY_PERCENT)
                )
        );
    }

    private void givenBudgetIsSaved() {
        when(repository.save(any(PresupuestoAlumno.class))).thenAnswer(invocation -> {
            PresupuestoAlumno budget = invocation.getArgument(0);
            budget.setId(randomUUID());
            budget.getReglasCategoria().forEach(rule -> rule.setId(randomUUID()));
            return budget;
        });
    }

    private void whenCreateBudget() {
        createBudgetResponse = createBudget.execute(createBudgetRequest);
    }

    private <T extends Throwable> void whenCreateBudgetFails(Class<T> expectedException) {
        Executable execution = () -> createBudget.execute(createBudgetRequest);
        thrownException = assertThrows(expectedException, execution);
    }

    private void thenBudgetWasSavedWithCalculatedRule() {
        verify(repository).save(budgetCaptor.capture());
        PresupuestoAlumno savedBudget = budgetCaptor.getValue();

        assertNotNull(savedBudget);
        assertTrue(savedBudget.isActivo());
        assertEquals(studentId, savedBudget.getAlumnoId());
        assertEquals(creatorId, savedBudget.getCreadoPorUsuarioId());
        assertEquals(LIMIT_AMOUNT, savedBudget.getMontoLimiteGeneral());
        assertEquals(PeriodoPresupuesto.MENSUAL, savedBudget.getPeriodo());
        assertEquals(START_DATE, savedBudget.getFechaInicio());
        assertEquals(1, savedBudget.getReglasCategoria().size());
        assertEquals(categoryId, savedBudget.getReglasCategoria().get(0).getCategoria().getId());
        assertEquals(TEN_PERCENT, savedBudget.getReglasCategoria().get(0).getPorcentajeLimite());
        assertEquals(
                CALCULATED_CATEGORY_LIMIT,
                savedBudget.getReglasCategoria().get(0).getMontoLimiteCalculado());
    }

    private void thenResponseContainsCreatedBudget() {
        PresupuestoAlumno savedBudget = budgetCaptor.getValue();

        assertNotNull(createBudgetResponse);
        assertEquals(savedBudget.getId(), createBudgetResponse.id());
        assertEquals(studentId, createBudgetResponse.alumnoId());
        assertEquals(creatorId, createBudgetResponse.creadoPorUsuarioId());
        assertEquals(LIMIT_AMOUNT, createBudgetResponse.montoLimiteGeneral());
        assertEquals(PeriodoPresupuesto.MENSUAL, createBudgetResponse.periodo());
        assertTrue(createBudgetResponse.activo());
        assertEquals(1, createBudgetResponse.reglasCategoria().size());
        assertEquals(
                CALCULATED_CATEGORY_LIMIT,
                createBudgetResponse.reglasCategoria().get(0).montoLimiteCalculado());
    }

    private void thenBudgetWasSavedWithoutRules() {
        verify(repository).save(budgetCaptor.capture());
        PresupuestoAlumno savedBudget = budgetCaptor.getValue();

        assertTrue(savedBudget.isActivo());
        assertTrue(savedBudget.getReglasCategoria().isEmpty());
    }

    private void thenResponseContainsBudgetWithoutRules() {
        assertEquals(studentId, createBudgetResponse.alumnoId());
        assertEquals(creatorId, createBudgetResponse.creadoPorUsuarioId());
        assertTrue(createBudgetResponse.reglasCategoria().isEmpty());
    }

    private void thenErrorMessageContains(String expectedMessage) {
        assertTrue(thrownException.getMessage().contains(expectedMessage));
    }

    private void thenBudgetWasNotSaved() {
        verify(repository, never()).save(any());
    }
}
