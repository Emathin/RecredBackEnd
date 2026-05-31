package com.recred.backend.infrastructure.persistence;

import com.recred.backend.application.DetectStudentPreferences;
import com.recred.backend.domain.models.Student;
import com.recred.backend.domain.repositories.StudentRepository;
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
    private StudentRepository studentRepository;
    @Mock
    private SpringDataBuffetRepository buffetRepository;
    @Mock
    private DetectStudentPreferences detectStudentPreferences;

    private PreferenceDetectionScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new PreferenceDetectionScheduler(
                studentRepository,
                buffetRepository,
                detectStudentPreferences
        );
    }

    @Test
    void runPreferenceDetectionForAllActiveStudentsInAllBuffets() {
        Student student1 = new Student();
        student1.setId(UUID.randomUUID());
        student1.setActive(true);

        Student student2 = new Student();
        student2.setId(UUID.randomUUID());
        student2.setActive(false);

        BuffetEntity buffet = new BuffetEntity();
        buffet.setId(UUID.randomUUID());

        when(studentRepository.findAll()).thenReturn(List.of(student1, student2));
        when(buffetRepository.findAll()).thenReturn(List.of(buffet));

        scheduler.runDetectionForAllStudents();

        verify(detectStudentPreferences, times(1)).execute(student1.getId(), buffet.getId());
        verify(detectStudentPreferences, never()).execute(student2.getId(), buffet.getId());
    }

    @Test
    void abortScheduledDetectionWhenNoBuffetsAreFound() {
        when(buffetRepository.findAll()).thenReturn(Collections.emptyList());

        scheduler.runDetectionForAllStudents();

        verify(detectStudentPreferences, never()).execute(any(), any());
    }

    @Test
    void handleIndividualExecutionErrorsWithoutInterruptingProcess() {
        Student student = new Student();
        student.setId(UUID.randomUUID());
        student.setActive(true);

        BuffetEntity buffet = new BuffetEntity();
        buffet.setId(UUID.randomUUID());

        when(studentRepository.findAll()).thenReturn(List.of(student));
        when(buffetRepository.findAll()).thenReturn(List.of(buffet));
        when(detectStudentPreferences.execute(any(), any())).thenThrow(new RuntimeException("Error IA"));

        scheduler.runDetectionForAllStudents();

        verify(detectStudentPreferences).execute(student.getId(), buffet.getId());
    }
}
