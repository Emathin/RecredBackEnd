package com.recred.backend.unit.application;

import com.recred.backend.application.ManualProductUnlock;
import com.recred.backend.application.ManualProductUnlockRequest;
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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManualProductUnlockTest {

    private static final UUID STUDENT_ID = UUID.randomUUID();
    private static final UUID PRODUCT_ID = UUID.randomUUID();

    @Mock
    private AlumnoRepository alumnoRepository;

    @InjectMocks
    private ManualProductUnlock manualProductUnlock;

    @Test
    void execute_ShouldUnlockProductAndSave() {
        ManualProductUnlockRequest request = givenRequest();
        Alumno alumno = givenExistingAlumno();

        whenExecute(request);

        thenAlumnoIsSaved(alumno);
    }

    @Test
    void execute_ShouldThrowExceptionWhenAlumnoNotFound() {
        ManualProductUnlockRequest request = givenRequest();
        givenNonExistingAlumno();

        Executable execution = whenExecuteExecutable(request);

        thenExceptionIsThrownAndNotSaved(execution);
    }

    private ManualProductUnlockRequest givenRequest() {
        return new ManualProductUnlockRequest(STUDENT_ID, PRODUCT_ID);
    }

    private Alumno givenExistingAlumno() {
        Alumno alumno = new Alumno();
        when(alumnoRepository.findById(STUDENT_ID)).thenReturn(Optional.of(alumno));
        return alumno;
    }

    private void givenNonExistingAlumno() {
        when(alumnoRepository.findById(STUDENT_ID)).thenReturn(Optional.empty());
    }

    private void whenExecute(ManualProductUnlockRequest request) {
        manualProductUnlock.execute(request);
    }

    private Executable whenExecuteExecutable(ManualProductUnlockRequest request) {
        return () -> manualProductUnlock.execute(request);
    }

    private void thenAlumnoIsSaved(Alumno alumno) {
        verify(alumnoRepository).save(alumno);
    }

    private void thenExceptionIsThrownAndNotSaved(Executable execution) {
        assertThrows(RuntimeException.class, execution);
        verify(alumnoRepository, never()).save(any(Alumno.class));
    }
}
