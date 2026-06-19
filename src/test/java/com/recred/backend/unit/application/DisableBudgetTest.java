package com.recred.backend.unit.application;

import com.recred.backend.application.DisableBudget;
import com.recred.backend.domain.models.PresupuestoAlumno;
import com.recred.backend.domain.repositories.PresupuestoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DisableBudgetTest {

    @Mock
    private PresupuestoRepository repository;

    @InjectMocks
    private DisableBudget disableBudget;

    @Test
    void executeWhenBudgetDoesNotExistShouldThrowException() {
        UUID budgetId = UUID.randomUUID();
        when(repository.findById(budgetId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> disableBudget.execute(budgetId));
    }

    @Test
    void executeWhenBudgetExistsShouldDeactivateAndSave() {
        UUID budgetId = UUID.randomUUID();
        PresupuestoAlumno budget = new PresupuestoAlumno();
        budget.setId(budgetId);
        budget.setActivo(true);

        when(repository.findById(budgetId)).thenReturn(Optional.of(budget));

        disableBudget.execute(budgetId);

        assertFalse(budget.isActivo(), "The budget should have been deactivated");
        verify(repository).save(budget);
    }
}
