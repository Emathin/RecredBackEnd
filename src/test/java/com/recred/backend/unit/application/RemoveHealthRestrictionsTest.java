package com.recred.backend.unit.application;

import com.recred.backend.application.RemoveHealthRestrictions;
import com.recred.backend.application.RemoveHealthRestrictionsRequest;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.repositories.AlumnoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RemoveHealthRestrictionsTest {

    private static final UUID STUDENT_ID = UUID.randomUUID();
    private static final UUID CLASSIFICATION_ID = UUID.randomUUID();

    @Mock
    private AlumnoRepository alumnoRepository;

    @InjectMocks
    private RemoveHealthRestrictions useCase;

    @Test
    void execute_WithValidData_ShouldRemoveAndSave() {
        RemoveHealthRestrictionsRequest request = givenRequest();
        Alumno student = givenStudentFound();

        whenExecute(request);

        thenRestrictionIsRemovedAndStudentSaved(student);
    }

    @Test
    void execute_WhenStudentNotFound_ShouldThrowException() {
        RemoveHealthRestrictionsRequest request = givenRequest();
        givenStudentNotFound();

        Executable execution = whenExecuteExecutable(request);

        thenStudentNotFoundExceptionIsThrown(execution);
    }

    private RemoveHealthRestrictionsRequest givenRequest() {
        return new RemoveHealthRestrictionsRequest(STUDENT_ID, CLASSIFICATION_ID);
    }

    private Alumno givenStudentFound() {
        Alumno student = mock(Alumno.class);
        when(alumnoRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));
        return student;
    }

    private void givenStudentNotFound() {
        when(alumnoRepository.findById(STUDENT_ID)).thenReturn(Optional.empty());
    }

    private void whenExecute(RemoveHealthRestrictionsRequest request) {
        useCase.execute(request);
    }

    private Executable whenExecuteExecutable(RemoveHealthRestrictionsRequest request) {
        return () -> useCase.execute(request);
    }

    private void thenRestrictionIsRemovedAndStudentSaved(Alumno student) {
        verify(student).removerRestriccionSalud(CLASSIFICATION_ID);
        verify(alumnoRepository).save(student);
    }

    private void thenStudentNotFoundExceptionIsThrown(Executable execution) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, execution);
        assertEquals("El alumno no existe.", exception.getMessage());
        verify(alumnoRepository, never()).save(any());
    }
}
