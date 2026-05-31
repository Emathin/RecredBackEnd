package com.recred.backend.application;

import com.recred.backend.domain.models.TimeRestriction;
import com.recred.backend.domain.repositories.TimeRestrictionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateTimeRestrictionTest {

    @Mock
    private TimeRestrictionRepository repository;

    @InjectMocks
    private UpdateTimeRestriction updateTimeRestriction;

    @Test
    void execute_ActualizaConNuevosFiltros() {
        UUID restrictionId = UUID.randomUUID();
        UUID nuevoTimeSlotId = UUID.randomUUID();
        UUID nuevaCategoriaId = UUID.randomUUID();
        UUID nuevaClasificacionId = UUID.randomUUID();

        TimeRestriction restriccionExistente = new TimeRestriction();
        when(repository.findById(restrictionId)).thenReturn(Optional.of(restriccionExistente));
        when(repository.save(any(TimeRestriction.class))).thenAnswer(i -> i.getArguments()[0]);

        TimeRestrictionCommand command = new TimeRestrictionCommand(
                UUID.randomUUID(), nuevoTimeSlotId, nuevaCategoriaId, nuevaClasificacionId);

        updateTimeRestriction.execute(restrictionId, command);

        ArgumentCaptor<TimeRestriction> captor = ArgumentCaptor.forClass(TimeRestriction.class);
        verify(repository).save(captor.capture());
        TimeRestriction guardada = captor.getValue();

        assertEquals(nuevoTimeSlotId, guardada.getTimeBadge().getId());
        assertEquals(nuevaCategoriaId, guardada.getCategory().getId());
        assertEquals(nuevaClasificacionId, guardada.getClassification().getId());
    }

    @Test
    void execute_LimpiaFiltros_CuandoCommandEnviaNulls() {
        UUID restrictionId = UUID.randomUUID();
        UUID nuevoTimeSlotId = UUID.randomUUID();

        TimeRestriction restriccionExistente = new TimeRestriction();

        when(repository.findById(restrictionId)).thenReturn(Optional.of(restriccionExistente));
        when(repository.save(any(TimeRestriction.class))).thenAnswer(i -> i.getArguments()[0]);

        TimeRestrictionCommand command = new TimeRestrictionCommand(
                UUID.randomUUID(), nuevoTimeSlotId, null, null);

        updateTimeRestriction.execute(restrictionId, command);

        ArgumentCaptor<TimeRestriction> captor = ArgumentCaptor.forClass(TimeRestriction.class);
        verify(repository).save(captor.capture());
        TimeRestriction guardada = captor.getValue();

        assertEquals(nuevoTimeSlotId, guardada.getTimeBadge().getId());
        assertNull(guardada.getCategory());
        assertNull(guardada.getClassification());
    }

    @Test
    void execute_LanzaExcepcion_CuandoNoExiste() {
        UUID restrictionId = UUID.randomUUID();
        TimeRestrictionCommand command = new TimeRestrictionCommand(UUID.randomUUID(), UUID.randomUUID(), null, null);

        when(repository.findById(restrictionId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> updateTimeRestriction.execute(restrictionId, command));
    }
}