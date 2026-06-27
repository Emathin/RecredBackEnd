package com.recred.backend.unit.application;

import com.recred.backend.application.DisableBudget;
import com.recred.backend.application.DisableBudgetRequest;
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
    void execute_PresupuestoNoExiste_LanzaExcepcion() {
        UUID budgetId = UUID.randomUUID();
        when(repository.findById(budgetId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> disableBudget.execute(new DisableBudgetRequest(budgetId)));
    }

    @Test
    void execute_PresupuestoExiste_LoDesactivaYGuarda() {
        UUID budgetId = UUID.randomUUID();
        PresupuestoAlumno presupuesto = new PresupuestoAlumno();
        presupuesto.setId(budgetId);
        presupuesto.setActivo(true);

        when(repository.findById(budgetId)).thenReturn(Optional.of(presupuesto));

        disableBudget.execute(new DisableBudgetRequest(budgetId));

        assertFalse(presupuesto.isActivo(), "El presupuesto debería haber sido desactivado");
        verify(repository).save(presupuesto);
    }
}
