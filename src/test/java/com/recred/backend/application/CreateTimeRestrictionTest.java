package com.recred.backend.application;

import com.recred.backend.domain.models.TimeRestriction;
import com.recred.backend.domain.repositories.TimeRestrictionRepository;
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
    private TimeRestrictionRepository repository;

    @InjectMocks
    private CreateTimeRestriction createTimeRestriction;

    @Test
    void execute_CreaRestriccionGeneral_CuandoNoHayFiltros() {
        UUID studentId = UUID.randomUUID();
        UUID timeSlotId = UUID.randomUUID();

        TimeRestrictionCommand command = new TimeRestrictionCommand(studentId, timeSlotId, null, null);

        when(repository.save(any(TimeRestriction.class))).thenAnswer(i -> i.getArguments()[0]);

        TimeRestriction result = createTimeRestriction.execute(command);

        ArgumentCaptor<TimeRestriction> captor = ArgumentCaptor.forClass(TimeRestriction.class);
        verify(repository).save(captor.capture());
        TimeRestriction guardada = captor.getValue();

        assertEquals(studentId, guardada.getStudentId());
        assertEquals(timeSlotId, guardada.getTimeBadge().getId());
        assertNull(guardada.getCategory());
        assertNull(guardada.getClassification());
        assertNotNull(result);
    }

    @Test
    void execute_CreaRestriccionEspecifica_CuandoSeEnvianFiltros() {
        UUID studentId = UUID.randomUUID();
        UUID timeSlotId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        UUID classificationId = UUID.randomUUID();

        TimeRestrictionCommand command = new TimeRestrictionCommand(studentId, timeSlotId, categoryId, classificationId);

        when(repository.save(any(TimeRestriction.class))).thenAnswer(i -> i.getArguments()[0]);

        createTimeRestriction.execute(command);

        ArgumentCaptor<TimeRestriction> captor = ArgumentCaptor.forClass(TimeRestriction.class);
        verify(repository).save(captor.capture());
        TimeRestriction guardada = captor.getValue();

        assertEquals(categoryId, guardada.getCategory().getId());
        assertEquals(classificationId, guardada.getClassification().getId());
    }
}