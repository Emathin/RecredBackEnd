package com.recred.backend.application;

import com.recred.backend.domain.models.TimeRestriction;
import com.recred.backend.domain.repositories.TimeRestrictionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetTimeRestrictionsByStudentTest {

    @Mock
    private TimeRestrictionRepository repository;

    @InjectMocks
    private GetTimeRestrictionsByStudent getTimeRestrictionsByStudent;

    @Test
    void execute_DevuelveListaDeRestriccionesDelAlumno() {
        UUID studentId = UUID.randomUUID();
        List<TimeRestriction> restriccionesEsperadas = List.of(new TimeRestriction());

        when(repository.findAllByAlumnoId(studentId)).thenReturn(restriccionesEsperadas);

        List<TimeRestriction> resultado = getTimeRestrictionsByStudent.execute(studentId);

        assertEquals(1, resultado.size());
        verify(repository).findAllByAlumnoId(studentId);
    }
}