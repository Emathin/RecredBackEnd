package com.recred.backend.unit.application;

import com.recred.backend.application.GetHealthRestrictions;
import com.recred.backend.application.GetHealthRestrictionsRequest;
import com.recred.backend.application.GetHealthRestrictionsResponse;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.ClasificacionSalud;
import com.recred.backend.domain.repositories.AlumnoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetHealthRestrictionsTest {

    private static final UUID STUDENT_ID = UUID.randomUUID();
    private static final UUID RESTRICTION_ID = UUID.randomUUID();
    private static final String RESTRICTION_DESCRIPTION = "Celíaco";

    @Mock
    private AlumnoRepository alumnoRepository;

    @InjectMocks
    private GetHealthRestrictions useCase;

    @Test
    void returnsRestrictionsWhenStudentExists() {
        Alumno student = buildStudentWithRestriction();
        givenStudentExists(student);
        GetHealthRestrictionsRequest request = new GetHealthRestrictionsRequest(STUDENT_ID);

        Set<GetHealthRestrictionsResponse> result = whenExecute(request);

        thenResultMatchesRestriction(result);
        thenRepositoryWasQueriedByStudentId();
    }

    @Test
    void throwsExceptionWhenStudentDoesNotExist() {
        givenStudentDoesNotExist();
        GetHealthRestrictionsRequest request = new GetHealthRestrictionsRequest(STUDENT_ID);

        Executable execution = () -> whenExecute(request);

        thenThrowsStudentNotFoundException(execution);
        thenRepositoryWasQueriedByStudentId();
    }

    private Alumno buildStudentWithRestriction() {
        Alumno student = new Alumno();
        student.setId(STUDENT_ID);

        ClasificacionSalud restriction = new ClasificacionSalud();
        restriction.setId(RESTRICTION_ID);
        restriction.setDescripcion(RESTRICTION_DESCRIPTION);
        
        student.getRestriccionesSalud().add(restriction);
        return student;
    }

    private void givenStudentExists(Alumno student) {
        when(alumnoRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));
    }

    private void givenStudentDoesNotExist() {
        when(alumnoRepository.findById(STUDENT_ID)).thenReturn(Optional.empty());
    }

    private Set<GetHealthRestrictionsResponse> whenExecute(GetHealthRestrictionsRequest request) {
        return useCase.execute(request);
    }

    private void thenResultMatchesRestriction(Set<GetHealthRestrictionsResponse> result) {
        assertNotNull(result);
        assertEquals(1, result.size());
        
        GetHealthRestrictionsResponse responseItem = result.iterator().next();
        assertEquals(RESTRICTION_ID, responseItem.id());
        assertEquals(RESTRICTION_DESCRIPTION, responseItem.descripcion());
    }

    private void thenRepositoryWasQueriedByStudentId() {
        verify(alumnoRepository).findById(STUDENT_ID);
    }

    private void thenThrowsStudentNotFoundException(Executable execution) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, execution);
        assertEquals("El alumno con ID " + STUDENT_ID + " no existe.", exception.getMessage());
    }
}
