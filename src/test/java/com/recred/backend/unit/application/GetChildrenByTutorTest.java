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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetChildrenByTutorTest {

    private static final UUID TUTOR_ID = UUID.randomUUID();
    private static final UUID CHILD_ID = UUID.randomUUID();
    private static final String COGNITO_SUBJECT = "cognito-sub";

    @Mock
    private TutorRepository tutorRepository;

    @InjectMocks
    private GetChildrenByTutor getChildrenByTutor;

    @Test
    void returnsChildrenWhenTutorExists() {
        Tutor tutor = buildTutorWithChild();
        givenTutorExistsById(tutor);
        GetChildrenByTutorRequest request = GetChildrenByTutorRequest.byTutorId(TUTOR_ID);

        List<GetChildrenByTutorResponse> result = whenExecute(request);

        thenResultContainsChild(result);
        thenRepositoryWasQueriedById();
    }

    @Test
    void throwsExceptionWhenTutorDoesNotExistById() {
        givenTutorDoesNotExistById();
        GetChildrenByTutorRequest request = GetChildrenByTutorRequest.byTutorId(TUTOR_ID);

        Executable execution = () -> whenExecute(request);

        thenThrowsTutorNotFoundByIdException(execution);
        thenRepositoryWasQueriedById();
    }

    @Test
    void returnsChildrenWhenTutorExistsByCognitoSubject() {
        Tutor tutor = buildTutorWithChild();
        givenTutorExistsByCognitoSubject(tutor);
        GetChildrenByTutorRequest request = GetChildrenByTutorRequest.byCognitoSubject(COGNITO_SUBJECT);

        List<GetChildrenByTutorResponse> result = whenExecute(request);

        thenResultContainsChild(result);
        thenRepositoryWasQueriedByCognitoSubject();
    }

    @Test
    void throwsExceptionWhenTutorDoesNotExistByCognitoSubject() {
        givenTutorDoesNotExistByCognitoSubject();
        GetChildrenByTutorRequest request = GetChildrenByTutorRequest.byCognitoSubject(COGNITO_SUBJECT);

        Executable execution = () -> whenExecute(request);

        thenThrowsTutorNotFoundByCognitoSubjectException(execution);
        thenRepositoryWasQueriedByCognitoSubject();
    }

    @Test
    void throwsExceptionWhenBothParametersAreNull() {
        GetChildrenByTutorRequest request = new GetChildrenByTutorRequest(null, null);

        Executable execution = () -> whenExecute(request);

        thenThrowsMissingParametersException(execution);
    }

    private Tutor buildTutorWithChild() {
        Tutor tutor = new Tutor();
        tutor.setId(TUTOR_ID);
        Alumno child = new Alumno();
        child.setId(CHILD_ID);
        tutor.addHijo(child);
        return tutor;
    }

    private void givenTutorExistsById(Tutor tutor) {
        when(tutorRepository.findById(TUTOR_ID)).thenReturn(Optional.of(tutor));
    }

    private void givenTutorDoesNotExistById() {
        when(tutorRepository.findById(TUTOR_ID)).thenReturn(Optional.empty());
    }

    private void givenTutorExistsByCognitoSubject(Tutor tutor) {
        when(tutorRepository.findByUsuarioIdCognito(COGNITO_SUBJECT)).thenReturn(Optional.of(tutor));
    }

    private void givenTutorDoesNotExistByCognitoSubject() {
        when(tutorRepository.findByUsuarioIdCognito(COGNITO_SUBJECT)).thenReturn(Optional.empty());
    }

    private List<GetChildrenByTutorResponse> whenExecute(GetChildrenByTutorRequest request) {
        return getChildrenByTutor.execute(request);
    }

    private void thenResultContainsChild(List<GetChildrenByTutorResponse> result) {
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(CHILD_ID, result.get(0).id());
    }

    private void thenRepositoryWasQueriedById() {
        verify(tutorRepository).findById(TUTOR_ID);
    }

    private void thenRepositoryWasQueriedByCognitoSubject() {
        verify(tutorRepository).findByUsuarioIdCognito(COGNITO_SUBJECT);
    }

    private void thenThrowsTutorNotFoundByIdException(Executable execution) {
        RuntimeException exception = assertThrows(RuntimeException.class, execution);
        assertEquals("Tutor no encontrado con ID: " + TUTOR_ID, exception.getMessage());
    }

    private void thenThrowsTutorNotFoundByCognitoSubjectException(Executable execution) {
        RuntimeException exception = assertThrows(RuntimeException.class, execution);
        assertEquals("Tutor no encontrado para el usuario autenticado", exception.getMessage());
    }

    private void thenThrowsMissingParametersException(Executable execution) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, execution);
        assertEquals("Debe proporcionar tutorId o cognitoSubject", exception.getMessage());
    }
}
