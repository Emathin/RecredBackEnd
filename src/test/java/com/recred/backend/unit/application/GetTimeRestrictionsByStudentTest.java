package com.recred.backend.unit.application;

import com.recred.backend.application.GetTimeRestrictionsByStudent;
import com.recred.backend.application.GetTimeRestrictionsByStudentRequest;
import com.recred.backend.application.GetTimeRestrictionsByStudentResponse;
import com.recred.backend.domain.models.RestriccionHoraria;
import com.recred.backend.domain.repositories.RestriccionHorariaRepository;
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
    private RestriccionHorariaRepository repository;

    @InjectMocks
    private GetTimeRestrictionsByStudent getTimeRestrictionsByStudent;

    @Test
    void execute_DevuelveListaDeRestriccionesDelAlumno() {
        UUID studentId = UUID.randomUUID();
        RestriccionHoraria restriccion = new RestriccionHoraria();
        restriccion.setId(UUID.randomUUID());
        restriccion.setAlumnoId(studentId);
        List<RestriccionHoraria> restriccionesEsperadas = List.of(restriccion);

        when(repository.findAllByAlumnoEntityId(studentId)).thenReturn(restriccionesEsperadas);

        List<GetTimeRestrictionsByStudentResponse> resultado =
                getTimeRestrictionsByStudent.execute(new GetTimeRestrictionsByStudentRequest(studentId));

        assertEquals(1, resultado.size());
        assertEquals(restriccion.getId(), resultado.get(0).id());
        assertEquals(studentId, resultado.get(0).alumnoId());
        verify(repository).findAllByAlumnoEntityId(studentId);
    }

    @Test
    void execute_CuandoNoHayRestricciones_RetornaListaVacia() {
        UUID studentId = UUID.randomUUID();
        when(repository.findAllByAlumnoEntityId(studentId)).thenReturn(List.of());

        List<GetTimeRestrictionsByStudentResponse> resultado =
                getTimeRestrictionsByStudent.execute(new GetTimeRestrictionsByStudentRequest(studentId));

        assertEquals(0, resultado.size());
        verify(repository).findAllByAlumnoEntityId(studentId);
    }
}
