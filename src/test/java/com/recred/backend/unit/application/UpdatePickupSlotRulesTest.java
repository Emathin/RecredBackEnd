package com.recred.backend.unit.application;

import com.recred.backend.application.UpdatePickupSlotRules;
import com.recred.backend.domain.exceptions.PickupSlotRulesNotFoundException;
import com.recred.backend.domain.models.FranjaHoraria;
import com.recred.backend.domain.repositories.FranjaHorariaRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UpdatePickupSlotRulesTest {

    @Test
    void execute_ShouldUpdateCapacityAndCutoff() {
        FranjaHorariaRepository repository = mock(FranjaHorariaRepository.class);
        UpdatePickupSlotRules useCase = new UpdatePickupSlotRules(repository);
        UUID slotId = UUID.randomUUID();
        FranjaHoraria slot = new FranjaHoraria();

        when(repository.findById(slotId)).thenReturn(Optional.of(slot));
        when(repository.save(slot)).thenReturn(slot);

        FranjaHoraria result = useCase.execute(slotId, 20, 15);

        assertEquals(20, result.getCupoMaximo());
        assertEquals(15, result.getMinutosCorte());
        verify(repository).save(slot);
    }

    @Test
    void execute_WhenCutoffIsNull_ShouldStoreZero() {
        FranjaHorariaRepository repository = mock(FranjaHorariaRepository.class);
        UpdatePickupSlotRules useCase = new UpdatePickupSlotRules(repository);
        UUID slotId = UUID.randomUUID();
        FranjaHoraria slot = new FranjaHoraria();

        when(repository.findById(slotId)).thenReturn(Optional.of(slot));
        when(repository.save(slot)).thenReturn(slot);

        FranjaHoraria result = useCase.execute(slotId, 20, null);

        assertEquals(0, result.getMinutosCorte());
    }

    @Test
    void execute_WhenCutoffIsNegative_ShouldThrowException() {
        FranjaHorariaRepository repository = mock(FranjaHorariaRepository.class);
        UpdatePickupSlotRules useCase = new UpdatePickupSlotRules(repository);
        UUID slotId = UUID.randomUUID();

        when(repository.findById(slotId)).thenReturn(Optional.of(new FranjaHoraria()));

        assertThrows(IllegalArgumentException.class,
                () -> useCase.execute(slotId, 20, -1));
    }

    @Test
    void execute_WhenSlotDoesNotExist_ShouldThrowException() {
        FranjaHorariaRepository repository = mock(FranjaHorariaRepository.class);
        UpdatePickupSlotRules useCase = new UpdatePickupSlotRules(repository);
        UUID slotId = UUID.randomUUID();

        when(repository.findById(slotId)).thenReturn(Optional.empty());

        assertThrows(PickupSlotRulesNotFoundException.class,
                () -> useCase.execute(slotId, 20, 15));
    }
}
