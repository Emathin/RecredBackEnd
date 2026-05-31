package com.recred.backend.application;

import com.recred.backend.domain.models.StudentBudget;
import com.recred.backend.domain.repositories.BudgetRepository;
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
    private BudgetRepository repository;

    @InjectMocks
    private DisableBudget disableBudget;

    @Test
    void execute_PresupuestoNoExiste_LanzaExcepcion() {
        UUID budgetId = UUID.randomUUID();
        when(repository.findById(budgetId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> disableBudget.execute(budgetId));
    }

    @Test
    void execute_PresupuestoExiste_LoDesactivaYGuarda() {
        UUID budgetId = UUID.randomUUID();
        StudentBudget presupuesto = new StudentBudget();
        presupuesto.setId(budgetId);
        presupuesto.setActive(true);

        when(repository.findById(budgetId)).thenReturn(Optional.of(presupuesto));

        disableBudget.execute(budgetId);

        assertFalse(presupuesto.isActive(), "El presupuesto debería haber sido desactivado");
        verify(repository).save(presupuesto);
    }
}