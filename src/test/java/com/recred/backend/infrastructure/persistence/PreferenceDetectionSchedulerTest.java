package com.recred.backend.infrastructure.persistence;

import com.recred.backend.application.DetectStudentPreferences;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.infrastructure.entities.BuffetEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PreferenceDetectionSchedulerTest {

    @Mock
    private AlumnoRepository alumnoRepository;
    @Mock
    private SpringDataBuffetRepository buffetRepository;
    @Mock
    private DetectStudentPreferences detectStudentPreferences;

    private PreferenceDetectionScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new PreferenceDetectionScheduler(
                alumnoRepository,
                buffetRepository,
                detectStudentPreferences
        );
    }

    @Test
    void runPreferenceDetectionForAllActiveStudentsInAllBuffets() {
        Alumno alumno1 = new Alumno();
        alumno1.setId(UUID.randomUUID());
        alumno1.setActivo(true);

        Alumno alumno2 = new Alumno();
        alumno2.setId(UUID.randomUUID());
        alumno2.setActivo(false);

        BuffetEntity buffet = new BuffetEntity();
        buffet.setId(UUID.randomUUID());

        when(alumnoRepository.findAll()).thenReturn(List.of(alumno1, alumno2));
        when(buffetRepository.findAll()).thenReturn(List.of(buffet));

        scheduler.runDetectionForAllStudents();

        verify(detectStudentPreferences, times(1)).execute(alumno1.getId(), buffet.getId());
        verify(detectStudentPreferences, never()).execute(alumno2.getId(), buffet.getId());
    }

    @Test
    void abortScheduledDetectionWhenNoBuffetsAreFound() {
        when(buffetRepository.findAll()).thenReturn(Collections.emptyList());

        scheduler.runDetectionForAllStudents();

        verify(detectStudentPreferences, never()).execute(any(), any());
    }

    @Test
    void handleIndividualExecutionErrorsWithoutInterruptingProcess() {
        Alumno alumno = new Alumno();
        alumno.setId(UUID.randomUUID());
        alumno.setActivo(true);

        BuffetEntity buffet = new BuffetEntity();
        buffet.setId(UUID.randomUUID());

        when(alumnoRepository.findAll()).thenReturn(List.of(alumno));
        when(buffetRepository.findAll()).thenReturn(List.of(buffet));
        when(detectStudentPreferences.execute(any(), any())).thenThrow(new RuntimeException("Error IA"));

        scheduler.runDetectionForAllStudents();

        verify(detectStudentPreferences).execute(alumno.getId(), buffet.getId());
    }
}
