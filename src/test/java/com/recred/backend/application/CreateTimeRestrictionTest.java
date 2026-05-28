package com.recred.backend.application;

import com.recred.backend.domain.models.RestriccionHoraria;
import com.recred.backend.domain.repositories.RestriccionHorariaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateTimeRestrictionTest {

    @Mock
    private RestriccionHorariaRepository repository;

    @InjectMocks
    private CreateTimeRestriction createTimeRestriction;

    @Test
    void execute_CreaRestriccionGeneral_CuandoNoHayFiltros() {
        UUID studentId = UUID.randomUUID();
        UUID timeSlotId = UUID.randomUUID();

        TimeRestrictionCommand command = new TimeRestrictionCommand(studentId, timeSlotId, null, null);

        when(repository.save(any(RestriccionHoraria.class))).thenAnswer(i -> i.getArguments()[0]);

        RestriccionHoraria result = createTimeRestriction.execute(command);

        ArgumentCaptor<RestriccionHoraria> captor = ArgumentCaptor.forClass(RestriccionHoraria.class);
        verify(repository).save(captor.capture());
        RestriccionHoraria guardada = captor.getValue();

        assertEquals(studentId, guardada.getAlumnoId());
        assertEquals(timeSlotId, guardada.getFranjaHoraria().getId());
        assertNull(guardada.getCategoria());
        assertNull(guardada.getClasificacion());
        assertNotNull(result);
    }

    @Test
    void execute_CreaRestriccionEspecifica_CuandoSeEnvianFiltros() {
        UUID studentId = UUID.randomUUID();
        UUID timeSlotId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        UUID classificationId = UUID.randomUUID();

        TimeRestrictionCommand command = new TimeRestrictionCommand(studentId, timeSlotId, categoryId, classificationId);

        when(repository.save(any(RestriccionHoraria.class))).thenAnswer(i -> i.getArguments()[0]);

        createTimeRestriction.execute(command);

        ArgumentCaptor<RestriccionHoraria> captor = ArgumentCaptor.forClass(RestriccionHoraria.class);
        verify(repository).save(captor.capture());
        RestriccionHoraria guardada = captor.getValue();

        assertEquals(categoryId, guardada.getCategoria().getId());
        assertEquals(classificationId, guardada.getClasificacion().getId());
    }
}