package com.recred.backend.unit.application;

import com.recred.backend.application.DisableBudget;
import com.recred.backend.application.DisableBudgetRequest;
import com.recred.backend.domain.models.PresupuestoAlumno;
import com.recred.backend.domain.repositories.PresupuestoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
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
        DisableBudgetRequest request = new DisableBudgetRequest(budgetId);

        when(repository.findById(budgetId)).thenReturn(Optional.empty());

        Executable execution = () -> disableBudget.execute(request);

        assertThrows(RuntimeException.class, execution);
        verify(repository, never()).save(any());
    }

    @Test
    void execute_PresupuestoExiste_LoDesactivaYGuarda() {
        UUID budgetId = UUID.randomUUID();
        PresupuestoAlumno presupuesto = new PresupuestoAlumno();
        presupuesto.setId(budgetId);
        presupuesto.setActivo(true);
        DisableBudgetRequest request = new DisableBudgetRequest(budgetId);

        when(repository.findById(budgetId)).thenReturn(Optional.of(presupuesto));

        disableBudget.execute(request);

        assertFalse(presupuesto.isActivo(), "El presupuesto debería haber sido desactivado");
        verify(repository).save(presupuesto);
    }
}
