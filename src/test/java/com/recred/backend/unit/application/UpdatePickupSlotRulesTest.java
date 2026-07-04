package com.recred.backend.unit.application;

import com.recred.backend.application.UpdatePickupSlotRules;
import com.recred.backend.application.UpdatePickupSlotRulesRequest;
import com.recred.backend.application.UpdatePickupSlotRulesResponse;
import com.recred.backend.domain.exceptions.PickupSlotRulesNotFoundException;
import com.recred.backend.domain.models.FranjaHoraria;
import com.recred.backend.domain.repositories.FranjaHorariaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

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
        UpdatePickupSlotRulesRequest request = new UpdatePickupSlotRulesRequest(slotId, 20, 15);

        when(repository.findById(slotId)).thenReturn(Optional.of(slot));
        when(repository.save(slot)).thenReturn(slot);

        UpdatePickupSlotRulesResponse result = useCase.execute(request);

        assertEquals(20, result.cupoMaximo());
        assertEquals(15, result.minutosCorte());
        verify(repository).save(slot);
    }

    @Test
    void execute_WhenCutoffIsNull_ShouldStoreZero() {
        FranjaHorariaRepository repository = mock(FranjaHorariaRepository.class);
        UpdatePickupSlotRules useCase = new UpdatePickupSlotRules(repository);
        UUID slotId = UUID.randomUUID();
        FranjaHoraria slot = new FranjaHoraria();
        UpdatePickupSlotRulesRequest request = new UpdatePickupSlotRulesRequest(slotId, 20, null);

        when(repository.findById(slotId)).thenReturn(Optional.of(slot));
        when(repository.save(slot)).thenReturn(slot);

        UpdatePickupSlotRulesResponse result = useCase.execute(request);

        assertEquals(0, result.minutosCorte());
    }

    @Test
    void execute_WhenCutoffIsNegative_ShouldThrowException() {
        FranjaHorariaRepository repository = mock(FranjaHorariaRepository.class);
        UpdatePickupSlotRules useCase = new UpdatePickupSlotRules(repository);
        UUID slotId = UUID.randomUUID();
        UpdatePickupSlotRulesRequest request = new UpdatePickupSlotRulesRequest(slotId, 20, -1);

        when(repository.findById(slotId)).thenReturn(Optional.of(new FranjaHoraria()));

        Executable execution = () -> useCase.execute(request);

        assertThrows(IllegalArgumentException.class, execution);
    }

    @Test
    void execute_WhenSlotDoesNotExist_ShouldThrowException() {
        FranjaHorariaRepository repository = mock(FranjaHorariaRepository.class);
        UpdatePickupSlotRules useCase = new UpdatePickupSlotRules(repository);
        UUID slotId = UUID.randomUUID();
        UpdatePickupSlotRulesRequest request = new UpdatePickupSlotRulesRequest(slotId, 20, 15);

        when(repository.findById(slotId)).thenReturn(Optional.empty());

        Executable execution = () -> useCase.execute(request);

        assertThrows(PickupSlotRulesNotFoundException.class, execution);
    }
}
