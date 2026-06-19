package com.recred.backend.unit.application;

import com.recred.backend.application.GetChildrenByParent;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.repositories.TutorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class GetChildrenByParentTest {

    @Mock
    private TutorRepository tutorRepository;

    @InjectMocks
    private GetChildrenByParent getChildrenByParent;

    @Test
    void dadoUnTutorExistente_cuandoSeEjecuta_entoncesRetornaSusHijos() {
        UUID tutorId = UUID.randomUUID();
        Tutor tutor = new Tutor();
        tutor.setId(tutorId);

        Alumno hijo1 = new Alumno();
        hijo1.setId(UUID.randomUUID());
        tutor.addHijo(hijo1);

        when(tutorRepository.findById(tutorId)).thenReturn(Optional.of(tutor));

        Set<Alumno> resultado = getChildrenByParent.execute(tutorId);

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        verify(tutorRepository, times(1)).findById(tutorId);
    }

    @Test
    void dadoUnTutorInexistente_cuandoSeEjecuta_entoncesLanzaExcepcion() {
        UUID tutorId = UUID.randomUUID();

        when(tutorRepository.findById(tutorId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            getChildrenByParent.execute(tutorId);
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

        Set<Alumno> result = getChildrenByParent.executeByCognitoSubject(subject);

        assertEquals(Set.of(hijo), result);
    }

    @Test
    void dadoUnSubjectSinTutor_cuandoSeEjecutaPorCognito_entoncesLanzaExcepcion() {
        String subject = "cognito-sub";
        when(tutorRepository.findByUsuarioIdCognito(subject)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> getChildrenByParent.executeByCognitoSubject(subject));

        assertEquals("Tutor no encontrado para el usuario autenticado", exception.getMessage());
    }
}
