package com.recred.backend.application;

import com.recred.backend.domain.models.RestriccionHoraria;
import com.recred.backend.domain.repositories.RestriccionHorariaRepository;
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
    private RestriccionHorariaRepository repository;

    @InjectMocks
    private UpdateTimeRestriction updateTimeRestriction;

    @Test
    void execute_ActualizaConNuevosFiltros() {
        UUID restrictionId = UUID.randomUUID();
        UUID nuevoTimeSlotId = UUID.randomUUID();
        UUID nuevaCategoriaId = UUID.randomUUID();
        UUID nuevaClasificacionId = UUID.randomUUID();

        RestriccionHoraria restriccionExistente = new RestriccionHoraria();
        when(repository.findById(restrictionId)).thenReturn(Optional.of(restriccionExistente));
        when(repository.save(any(RestriccionHoraria.class))).thenAnswer(i -> i.getArguments()[0]);

        TimeRestrictionCommand command = new TimeRestrictionCommand(
                UUID.randomUUID(), nuevoTimeSlotId, nuevaCategoriaId, nuevaClasificacionId);

        updateTimeRestriction.execute(restrictionId, command);

        ArgumentCaptor<RestriccionHoraria> captor = ArgumentCaptor.forClass(RestriccionHoraria.class);
        verify(repository).save(captor.capture());
        RestriccionHoraria guardada = captor.getValue();

        assertEquals(nuevoTimeSlotId, guardada.getFranjaHoraria().getId());
        assertEquals(nuevaCategoriaId, guardada.getCategoria().getId());
        assertEquals(nuevaClasificacionId, guardada.getClasificacion().getId());
    }

    @Test
    void execute_LimpiaFiltros_CuandoCommandEnviaNulls() {
        UUID restrictionId = UUID.randomUUID();
        UUID nuevoTimeSlotId = UUID.randomUUID();

        RestriccionHoraria restriccionExistente = new RestriccionHoraria();

        when(repository.findById(restrictionId)).thenReturn(Optional.of(restriccionExistente));
        when(repository.save(any(RestriccionHoraria.class))).thenAnswer(i -> i.getArguments()[0]);

        TimeRestrictionCommand command = new TimeRestrictionCommand(
                UUID.randomUUID(), nuevoTimeSlotId, null, null);

        updateTimeRestriction.execute(restrictionId, command);

        ArgumentCaptor<RestriccionHoraria> captor = ArgumentCaptor.forClass(RestriccionHoraria.class);
        verify(repository).save(captor.capture());
        RestriccionHoraria guardada = captor.getValue();

        assertEquals(nuevoTimeSlotId, guardada.getFranjaHoraria().getId());
        assertNull(guardada.getCategoria());
        assertNull(guardada.getClasificacion());
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