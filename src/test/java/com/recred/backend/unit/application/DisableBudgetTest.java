package com.recred.backend.unit.application;

import com.recred.backend.application.DisableBudget;
import com.recred.backend.application.DisableBudgetRequest;
import com.recred.backend.domain.models.PresupuestoAlumno;
import com.recred.backend.domain.repositories.PresupuestoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DisableBudgetTest {

    @Mock
    private PresupuestoRepository repository;

    private DisableBudget disableBudget;
    private DisableBudgetRequest disableBudgetRequest;
    private PresupuestoAlumno budget;
    private UUID budgetId;
    private Throwable thrownException;

    @BeforeEach
    void setUp() {
        disableBudget = new DisableBudget(repository);
    }

    @Test
    void givenExistingBudget_whenDisablingBudget_thenDisablesAndSavesBudget() {
        givenActiveBudget();

        whenDisableBudget();

        thenBudgetWasDisabled();
        thenBudgetWasSaved();
    }

    @Test
    void givenMissingBudget_whenDisablingBudget_thenThrowsException() {
        givenMissingBudget();

        whenDisableBudgetFails(RuntimeException.class);

        thenErrorMessageIs("Presupuesto no encontrado con ID: " + budgetId);
        thenBudgetWasNotSaved();
    }

    private void givenActiveBudget() {
        budgetId = randomUUID();
        disableBudgetRequest = new DisableBudgetRequest(budgetId);
        budget = new PresupuestoAlumno();
        budget.setId(budgetId);
        budget.setActivo(true);
        when(repository.findById(budgetId)).thenReturn(Optional.of(budget));
    }

    private void givenMissingBudget() {
        budgetId = randomUUID();
        disableBudgetRequest = new DisableBudgetRequest(budgetId);
        when(repository.findById(budgetId)).thenReturn(Optional.empty());
    }

    private void whenDisableBudget() {
        disableBudget.execute(disableBudgetRequest);
    }

    private <T extends Throwable> void whenDisableBudgetFails(Class<T> expectedException) {
        Executable execution = () -> disableBudget.execute(disableBudgetRequest);
        thrownException = assertThrows(expectedException, execution);
    }

    private void thenBudgetWasDisabled() {
        assertFalse(budget.isActivo());
    }

    private void thenBudgetWasSaved() {
        verify(repository).save(budget);
    }

    private void thenBudgetWasNotSaved() {
        verify(repository, never()).save(any());
    }

    private void thenErrorMessageIs(String expectedMessage) {
        assertEquals(expectedMessage, thrownException.getMessage());
    }
}
