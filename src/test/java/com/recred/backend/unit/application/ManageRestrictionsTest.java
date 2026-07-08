package com.recred.backend.unit.application;

import com.recred.backend.application.ManageRestrictions;
import com.recred.backend.application.ManageRestrictionsRequest;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.ClasificacionSalud;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.ClasificacionSaludRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManageRestrictionsTest {

    @Mock
    private AlumnoRepository studentRepository;

    @Mock
    private ClasificacionSaludRepository healthClassificationRepository;

    private ManageRestrictions useCase;

    private UUID studentId;
    private UUID classificationId;
    private Alumno student;
    private ClasificacionSalud classification;
    private IllegalArgumentException thrownException;

    @BeforeEach
    void setUp() {
        useCase = new ManageRestrictions(studentRepository, healthClassificationRepository);
    }

    @Test
    void givenStudentAndHealthRestriction_whenManagingRestrictions_thenReplacesRestrictions() {
        givenManageRestrictionsScenario();
        givenStudentExists();
        givenHealthClassificationExists();

        whenManagingRestrictions();

        thenStudentHasOnlyRequestedRestriction();
        thenStudentWasSaved();
    }

    @Test
    void givenStudentDoesNotExist_whenManagingRestrictions_thenFailsBeforeSaving() {
        givenManageRestrictionsScenario();
        givenStudentDoesNotExist();

        whenManagingRestrictionsFails();

        thenStudentNotFoundIsReported();
        thenHealthClassificationsWereNotRequested();
        thenStudentWasNotSaved();
    }

    private void givenManageRestrictionsScenario() {
        studentId = UUID.randomUUID();
        classificationId = UUID.randomUUID();
        student = new Alumno();
        student.setId(studentId);
        classification = new ClasificacionSalud();
        classification.setId(classificationId);
    }

    private void givenStudentExists() {
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
    }

    private void givenStudentDoesNotExist() {
        when(studentRepository.findById(studentId)).thenReturn(Optional.empty());
    }

    private void givenHealthClassificationExists() {
        when(healthClassificationRepository.findAllById(List.of(classificationId)))
                .thenReturn(List.of(classification));
    }

    private void whenManagingRestrictions() {
        useCase.execute(request());
    }

    private void whenManagingRestrictionsFails() {
        thrownException = assertThrows(
                IllegalArgumentException.class,
                this::whenManagingRestrictions);
    }

    private void thenStudentHasOnlyRequestedRestriction() {
        assertTrue(student.getRestriccionesSalud().contains(classification));
        assertEquals(1, student.getRestriccionesSalud().size());
    }

    private void thenStudentWasSaved() {
        verify(studentRepository).save(student);
    }

    private void thenStudentNotFoundIsReported() {
        assertEquals("El alumno con ID " + studentId + " no existe.",
                thrownException.getMessage());
    }

    private void thenHealthClassificationsWereNotRequested() {
        verify(healthClassificationRepository, never()).findAllById(any());
    }

    private void thenStudentWasNotSaved() {
        verify(studentRepository, never()).save(any());
    }

    private ManageRestrictionsRequest request() {
        return new ManageRestrictionsRequest(studentId, List.of(classificationId));
    }
}
