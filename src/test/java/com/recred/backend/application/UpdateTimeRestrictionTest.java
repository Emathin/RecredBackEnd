package com.recred.backend.application;

import com.recred.backend.domain.models.RestriccionHoraria;
import com.recred.backend.domain.repositories.RestriccionHorariaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.repositories.AlumnoRepository;

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
    @Mock
    private AlumnoRepository alumnoRepository;

    @InjectMocks
    private UpdateTimeRestriction updateTimeRestriction;

    @Test
    void execute_ActualizaConNuevosFiltros() {
        UUID restrictionId = UUID.randomUUID();
        UUID nuevoTimeSlotId = UUID.randomUUID();
        UUID nuevaCategoriaId = UUID.randomUUID();
        UUID nuevaClasificacionId = UUID.randomUUID();

        TimeRestrictionCommand command = new TimeRestrictionCommand(
                UUID.randomUUID(), nuevoTimeSlotId, nuevaCategoriaId, nuevaClasificacionId);

        RestriccionHoraria restriccionExistente = new RestriccionHoraria();
        restriccionExistente.setAlumnoId(command.studentId());
        when(repository.findById(restrictionId)).thenReturn(Optional.of(restriccionExistente));
        when(repository.save(any(RestriccionHoraria.class))).thenAnswer(i -> i.getArguments()[0]);

        when(alumnoRepository.findById(command.studentId())).thenReturn(Optional.of(new Alumno()));

        updateTimeRestriction.execute(restrictionId, command);

        ArgumentCaptor<RestriccionHoraria> captor = ArgumentCaptor.forClass(RestriccionHoraria.class);
        verify(repository).save(captor.capture());
        RestriccionHoraria guardada = captor.getValue();

        assertEquals(nuevoTimeSlotId, guardada.getFranjaHoraria().getId());
        assertEquals(nuevaCategoriaId, guardada.getCategoria().getId());
        assertEquals(nuevaClasificacionId, guardada.getClasificacionSalud().getId());
    }

    @Test
    void execute_LimpiaFiltros_CuandoCommandEnviaNulls() {
        UUID restrictionId = UUID.randomUUID();
        UUID nuevoTimeSlotId = UUID.randomUUID();

        TimeRestrictionCommand command = new TimeRestrictionCommand(
                UUID.randomUUID(), nuevoTimeSlotId, null, null);

        RestriccionHoraria restriccionExistente = new RestriccionHoraria();
        restriccionExistente.setAlumnoId(command.studentId());

        when(repository.findById(restrictionId)).thenReturn(Optional.of(restriccionExistente));
        when(repository.save(any(RestriccionHoraria.class))).thenAnswer(i -> i.getArguments()[0]);

        when(alumnoRepository.findById(command.studentId())).thenReturn(Optional.of(new Alumno()));

        updateTimeRestriction.execute(restrictionId, command);

        ArgumentCaptor<RestriccionHoraria> captor = ArgumentCaptor.forClass(RestriccionHoraria.class);
        verify(repository).save(captor.capture());
        RestriccionHoraria guardada = captor.getValue();

        assertEquals(nuevoTimeSlotId, guardada.getFranjaHoraria().getId());
        assertNull(guardada.getCategoria());
        assertNull(guardada.getClasificacionSalud());
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