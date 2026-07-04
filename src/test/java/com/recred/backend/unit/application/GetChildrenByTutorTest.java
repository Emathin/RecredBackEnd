package com.recred.backend.unit.application;

import com.recred.backend.application.GetChildrenByTutor;
import com.recred.backend.application.GetChildrenByTutorRequest;
import com.recred.backend.application.GetChildrenByTutorResponse;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.repositories.TutorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
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
class GetChildrenByTutorTest {

    @Mock
    private TutorRepository tutorRepository;

    @InjectMocks
    private GetChildrenByTutor getChildrenByTutor;

    @Test
    void dadoUnTutorExistente_cuandoSeEjecuta_entoncesRetornaSusHijos() {
        UUID tutorId = UUID.randomUUID();
        Tutor tutor = new Tutor();
        tutor.setId(tutorId);

        Alumno hijo1 = new Alumno();
        hijo1.setId(UUID.randomUUID());
        tutor.addHijo(hijo1);
        GetChildrenByTutorRequest request = GetChildrenByTutorRequest.byTutorId(tutorId);

        when(tutorRepository.findById(tutorId)).thenReturn(Optional.of(tutor));

        List<GetChildrenByTutorResponse> resultado = getChildrenByTutor.execute(request);

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        assertEquals(hijo1.getId(), resultado.get(0).id());
        verify(tutorRepository, times(1)).findById(tutorId);
    }

    @Test
    void dadoUnTutorInexistente_cuandoSeEjecuta_entoncesLanzaExcepcion() {
        UUID tutorId = UUID.randomUUID();
        GetChildrenByTutorRequest request = GetChildrenByTutorRequest.byTutorId(tutorId);

        when(tutorRepository.findById(tutorId)).thenReturn(Optional.empty());

        Executable execution = () -> getChildrenByTutor.execute(request);

        RuntimeException exception = assertThrows(RuntimeException.class, execution);
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
        GetChildrenByTutorRequest request = GetChildrenByTutorRequest.byCognitoSubject(subject);

        when(tutorRepository.findByUsuarioIdCognito(subject)).thenReturn(Optional.of(tutor));

        List<GetChildrenByTutorResponse> result = getChildrenByTutor.execute(request);

        assertEquals(1, result.size());
        assertEquals(hijo.getId(), result.get(0).id());
    }

    @Test
    void dadoUnSubjectSinTutor_cuandoSeEjecutaPorCognito_entoncesLanzaExcepcion() {
        String subject = "cognito-sub";
        GetChildrenByTutorRequest request = GetChildrenByTutorRequest.byCognitoSubject(subject);

        when(tutorRepository.findByUsuarioIdCognito(subject)).thenReturn(Optional.empty());

        Executable execution = () -> getChildrenByTutor.execute(request);

        RuntimeException exception = assertThrows(RuntimeException.class, execution);
        assertEquals("Tutor no encontrado para el usuario autenticado", exception.getMessage());
    }

    @Test
    void execute_CuandoAmbosParametrosSonNulos_LanzaIllegalArgumentException() {
        GetChildrenByTutorRequest request = new GetChildrenByTutorRequest(null, null);

        Executable execution = () -> getChildrenByTutor.execute(request);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, execution);
        assertEquals("Debe proporcionar tutorId o cognitoSubject", exception.getMessage());
    }
}
