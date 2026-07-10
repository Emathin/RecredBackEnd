package com.recred.backend.unit.application;

import com.recred.backend.application.UpdateBudgetRules;
import com.recred.backend.application.UpdateBudgetRulesRequest;
import com.recred.backend.application.UpdateBudgetRulesResponse;
import com.recred.backend.domain.models.PresupuestoAlumno;
import com.recred.backend.domain.repositories.PresupuestoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateBudgetRulesTest {

    @Mock
    private PresupuestoRepository repository;

    @Captor
    private ArgumentCaptor<PresupuestoAlumno> budgetCaptor;

    @InjectMocks
    private UpdateBudgetRules updateBudgetRules;

    private UUID budgetId;
    private PresupuestoAlumno existingBudget;
    private UpdateBudgetRulesRequest updateRequest;
    private UpdateBudgetRulesResponse updateResponse;
    private Throwable thrownException;

    @BeforeEach
    void setUp() {
        budgetId = randomUUID();
        existingBudget = budget(budgetId, "5000.00");
    }

    @Test
    void givenMissingBudget_whenUpdatingRules_thenThrowsException() {
        givenBudgetDoesNotExist();
        givenEmptyRulesRequest();

        whenUpdateBudgetRulesFails(RuntimeException.class);

        thenBudgetWasNotSaved();
    }

    @Test
    void givenValidCategoryRules_whenUpdatingRules_thenSavesCalculatedRules() {
        givenBudgetExists();
        givenBudgetCanBeSaved();
        givenRulesRequest(rule("10.00"));

        whenUpdatingBudgetRules();

        thenBudgetWasSavedWithCalculatedRule("10.00", "500.00");
        thenResponseIncludesCalculatedRule("10.00", "500.00");
    }

    @Test
    void givenEmptyRules_whenUpdatingRules_thenClearsBudgetRules() {
        givenBudgetExists();
        givenBudgetCanBeSaved();
        givenEmptyRulesRequest();

        whenUpdatingBudgetRules();

        thenBudgetRulesWereCleared();
        thenResponseRulesWereCleared();
    }

    @Test
    void givenCategoryRulesOverOneHundredPercent_whenUpdatingRules_thenThrowsException() {
        givenBudgetExists();
        givenRulesRequest(rule("60.00"), rule("50.00"));

        whenUpdateBudgetRulesFails(IllegalArgumentException.class);

        thenErrorMessageContains("La suma de los porcentajes no puede superar el 100%");
        thenBudgetWasNotSaved();
    }

    private void givenBudgetExists() {
        when(repository.findById(budgetId)).thenReturn(Optional.of(existingBudget));
    }

    private void givenBudgetDoesNotExist() {
        when(repository.findById(budgetId)).thenReturn(Optional.empty());
    }

    private void givenBudgetCanBeSaved() {
        when(repository.save(any(PresupuestoAlumno.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void givenEmptyRulesRequest() {
        updateRequest = new UpdateBudgetRulesRequest(budgetId, List.of());
    }

    private void givenRulesRequest(UpdateBudgetRulesRequest.CategoryRuleRequest... rules) {
        updateRequest = new UpdateBudgetRulesRequest(budgetId, List.of(rules));
    }

    private void whenUpdatingBudgetRules() {
        updateResponse = updateBudgetRules.execute(updateRequest);
    }

    private <T extends Throwable> void whenUpdateBudgetRulesFails(Class<T> exceptionType) {
        Executable execution = () -> updateBudgetRules.execute(updateRequest);
        thrownException = assertThrows(exceptionType, execution);
    }

    private void thenBudgetWasSavedWithCalculatedRule(
            String expectedPercentage,
            String expectedAmount) {
        verify(repository).save(budgetCaptor.capture());
        PresupuestoAlumno savedBudget = budgetCaptor.getValue();
        assertEquals(1, savedBudget.getReglasCategoria().size());
        assertEquals(
                new BigDecimal(expectedPercentage),
                savedBudget.getReglasCategoria().get(0).getPorcentajeLimite());
        assertEquals(
                new BigDecimal(expectedAmount),
                savedBudget.getReglasCategoria().get(0).getMontoLimiteCalculado());
    }

    private void thenResponseIncludesCalculatedRule(
            String expectedPercentage,
            String expectedAmount) {
        assertEquals(budgetId, updateResponse.id());
        assertEquals(1, updateResponse.reglasCategoria().size());
        assertEquals(
                new BigDecimal(expectedPercentage),
                updateResponse.reglasCategoria().get(0).porcentajeLimite());
        assertEquals(
                new BigDecimal(expectedAmount),
                updateResponse.reglasCategoria().get(0).montoLimiteCalculado());
    }

    private void thenBudgetRulesWereCleared() {
        verify(repository).save(budgetCaptor.capture());
        assertTrue(budgetCaptor.getValue().getReglasCategoria().isEmpty());
    }

    private void thenResponseRulesWereCleared() {
        assertTrue(updateResponse.reglasCategoria().isEmpty());
    }

    private void thenBudgetWasNotSaved() {
        verify(repository, never()).save(any());
    }

    private void thenErrorMessageContains(String expectedMessage) {
        assertTrue(thrownException.getMessage().contains(expectedMessage));
    }

    private PresupuestoAlumno budget(UUID id, String generalLimit) {
        PresupuestoAlumno budget = new PresupuestoAlumno();
        budget.setId(id);
        budget.setMontoLimiteGeneral(new BigDecimal(generalLimit));
        return budget;
    }

    private UpdateBudgetRulesRequest.CategoryRuleRequest rule(String percentageLimit) {
        return new UpdateBudgetRulesRequest.CategoryRuleRequest(
                randomUUID(),
                new BigDecimal(percentageLimit));
    }
}
