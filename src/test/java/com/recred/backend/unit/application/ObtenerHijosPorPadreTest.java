package com.recred.backend.unit.application;

import com.recred.backend.application.ObtenerHijosPorPadre;
import com.recred.backend.application.ObtenerHijosPorPadreRequest;
import com.recred.backend.application.ObtenerHijosPorPadreResponse;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.repositories.TutorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class ObtenerHijosPorPadreTest {

    @Mock
    private TutorRepository tutorRepository;

    @InjectMocks
    private ObtenerHijosPorPadre obtenerHijosPorPadre;

    @Test
    void dadoUnTutorExistente_cuandoSeEjecuta_entoncesRetornaSusHijos() {
        UUID tutorId = UUID.randomUUID();
        Tutor tutor = new Tutor();
        tutor.setId(tutorId);

        Alumno hijo1 = new Alumno();
        hijo1.setId(UUID.randomUUID());
        tutor.addHijo(hijo1);

        when(tutorRepository.findById(tutorId)).thenReturn(Optional.of(tutor));

        List<ObtenerHijosPorPadreResponse> resultado = obtenerHijosPorPadre.execute(
                ObtenerHijosPorPadreRequest.byTutorId(tutorId));

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        assertEquals(hijo1.getId(), resultado.get(0).id());
        verify(tutorRepository, times(1)).findById(tutorId);
    }

    @Test
    void dadoUnTutorInexistente_cuandoSeEjecuta_entoncesLanzaExcepcion() {
        UUID tutorId = UUID.randomUUID();

        when(tutorRepository.findById(tutorId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            obtenerHijosPorPadre.execute(ObtenerHijosPorPadreRequest.byTutorId(tutorId));
        });

        assertEquals("Tutor no encontrado con ID: " + tutorId, exception.getMessage());
        verify(tutorRepository, times(1)).findById(tutorId);
    }

    @Test
    void dadoUnSubjectConTutor_cuandoSeEjecutaPorCognito_entoncesRetornaSusHijos() {
        String subject = "cognito-sub";
        Tutor tutor = new Tutor();
        Alumno hijo = new Alumno();
        hijo.setId(UUID.randomUUID());
        tutor.addHijo(hijo);
        when(tutorRepository.findByUsuarioIdCognito(subject)).thenReturn(Optional.of(tutor));

        List<ObtenerHijosPorPadreResponse> result = obtenerHijosPorPadre.execute(
                ObtenerHijosPorPadreRequest.byCognitoSubject(subject));

        assertEquals(1, result.size());
        assertEquals(hijo.getId(), result.get(0).id());
    }

    @Test
    void dadoUnSubjectSinTutor_cuandoSeEjecutaPorCognito_entoncesLanzaExcepcion() {
        String subject = "cognito-sub";
        when(tutorRepository.findByUsuarioIdCognito(subject)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> obtenerHijosPorPadre.execute(ObtenerHijosPorPadreRequest.byCognitoSubject(subject)));

        assertEquals("Tutor no encontrado para el usuario autenticado", exception.getMessage());
    }
}
