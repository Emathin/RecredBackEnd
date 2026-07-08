package com.recred.backend.unit.application;

import com.recred.backend.application.UpdateBudget;
import com.recred.backend.application.UpdateBudgetRequest;
import com.recred.backend.application.UpdateBudgetResponse;
import com.recred.backend.domain.models.PeriodoPresupuesto;
import com.recred.backend.domain.models.PresupuestoAlumno;
import com.recred.backend.domain.repositories.PresupuestoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;

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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UpdateBudgetTest {

    private static final UUID BUDGET_ID = UUID.randomUUID();
    private static final UUID CATEGORY_ID = UUID.randomUUID();
    private static final BigDecimal AMOUNT_LIMIT = new BigDecimal("10000.00");
    private static final PeriodoPresupuesto PERIOD = PeriodoPresupuesto.SEMANAL;
    private static final LocalDate START_DATE = LocalDate.now();

    private final PresupuestoRepository repository = mock(PresupuestoRepository.class);
    private final UpdateBudget updateBudget = new UpdateBudget(repository);

    private PresupuestoAlumno existingBudget;

    @BeforeEach
    void setUp() {
        existingBudget = new PresupuestoAlumno();
        existingBudget.setId(BUDGET_ID);
    }

    @Test
    void shouldThrowExceptionWhenBudgetNotFound() {
        givenBudgetNotFound();

        Executable execution = () -> whenUpdatingBudget(List.of());

        thenThrowsRuntimeException(execution);
        thenBudgetWasNotSaved();
    }

    @Test
    void shouldUpdateFieldsAndRecalculatePercentages() {
        givenExistingBudget();
        givenSavedBudgetReturnsItself();
        List<UpdateBudgetRequest.CategoryRuleRequest> rules = List.of(
                new UpdateBudgetRequest.CategoryRuleRequest(CATEGORY_ID, new BigDecimal("20.00"))
        );

        UpdateBudgetResponse response = whenUpdatingBudget(rules);

        thenBudgetIsSavedWithExpectedValues(response);
    }

    @Test
    void shouldThrowExceptionWhenPercentagesExceed100() {
        givenExistingBudget();
        List<UpdateBudgetRequest.CategoryRuleRequest> rules = List.of(
                new UpdateBudgetRequest.CategoryRuleRequest(UUID.randomUUID(), new BigDecimal("60.00")),
                new UpdateBudgetRequest.CategoryRuleRequest(UUID.randomUUID(), new BigDecimal("50.00"))
        );

        Executable execution = () -> whenUpdatingBudget(rules);

        thenThrowsIllegalArgumentException(execution);
        thenBudgetWasNotSaved();
    }

    // --- Helpers Privados ---

    private void givenExistingBudget() {
        when(repository.findById(BUDGET_ID)).thenReturn(Optional.of(existingBudget));
    }

    private void givenBudgetNotFound() {
        when(repository.findById(BUDGET_ID)).thenReturn(Optional.empty());
    }

    private void givenSavedBudgetReturnsItself() {
        when(repository.save(any(PresupuestoAlumno.class))).thenAnswer(i -> i.getArgument(0));
    }

    private UpdateBudgetResponse whenUpdatingBudget(
            List<UpdateBudgetRequest.CategoryRuleRequest> rules) {
        UpdateBudgetRequest request = new UpdateBudgetRequest(
                BUDGET_ID, AMOUNT_LIMIT, PERIOD, START_DATE, rules
        );
        return updateBudget.execute(request);
    }

    private void thenThrowsRuntimeException(Executable execution) {
        assertThrows(RuntimeException.class, execution);
    }

    private void thenThrowsIllegalArgumentException(Executable execution) {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class, execution);
        assertTrue(exception.getMessage().contains("La suma de los porcentajes no puede superar el 100%"));
    }

    private void thenBudgetWasNotSaved() {
        verify(repository, never()).save(any());
    }

    private void thenBudgetIsSavedWithExpectedValues(UpdateBudgetResponse response) {
        ArgumentCaptor<PresupuestoAlumno> captor = ArgumentCaptor.forClass(PresupuestoAlumno.class);
        verify(repository).save(captor.capture());
        PresupuestoAlumno saved = captor.getValue();

        assertEquals(AMOUNT_LIMIT, saved.getMontoLimiteGeneral());
        assertEquals(PERIOD, saved.getPeriodo());
        assertNotNull(saved.getReglasCategoria());
        assertEquals(1, saved.getReglasCategoria().size());
        assertEquals(new BigDecimal("2000.00"), 
                saved.getReglasCategoria().get(0).getMontoLimiteCalculado());

        assertEquals(BUDGET_ID, response.id());
        assertEquals(AMOUNT_LIMIT, response.montoLimiteGeneral());
        assertEquals(PERIOD, response.periodo());
        assertNotNull(response.reglasCategoria());
        assertEquals(1, response.reglasCategoria().size());
        assertEquals(new BigDecimal("2000.00"), 
                response.reglasCategoria().get(0).montoLimiteCalculado());
    }
}
