package com.recred.backend.unit.application;

import com.recred.backend.application.AddHealthRestriction;
import com.recred.backend.application.AddHealthRestrictionRequest;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.ClasificacionSalud;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.ClasificacionSaludRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddHealthRestrictionTest {

    @Mock
    private AlumnoRepository alumnoRepository;
    @Mock
    private ClasificacionSaludRepository clasificacionRepository;

    private AddHealthRestriction addHealthRestriction;
    private AddHealthRestrictionRequest addRestrictionRequest;
    private Alumno student;
    private ClasificacionSalud classification;
    private Throwable thrownException;

    @BeforeEach
    void setUp() {
        addHealthRestriction = new AddHealthRestriction(
                alumnoRepository,
                clasificacionRepository
        );
    }

    @Test
    void givenValidHealthRestrictionRequest_whenAddingRestriction_thenAddsAndSavesStudent() {
        givenDefaultHealthRestrictionRequest();
        givenStudentExists();
        givenClassificationExists();

        whenAddHealthRestriction();

        thenClassificationWasAddedToStudent();
        thenStudentWasSaved();
    }

    @Test
    void givenMissingStudent_whenAddingRestriction_thenThrowsException() {
        givenDefaultHealthRestrictionRequest();
        givenStudentDoesNotExist();

        whenAddHealthRestrictionFails(IllegalArgumentException.class);

        thenErrorMessageIs("El alumno no existe.");
        thenClassificationWasNotSearched();
        thenStudentWasNotSaved();
    }

    @Test
    void givenMissingClassification_whenAddingRestriction_thenThrowsException() {
        givenDefaultHealthRestrictionRequest();
        givenStudentExists();
        givenClassificationDoesNotExist();

        whenAddHealthRestrictionFails(IllegalArgumentException.class);

        thenErrorMessageIs("La clasificaciÃ³n mÃ©dica no existe.");
        thenStudentWasNotSaved();
    }

    @Test
    void givenExistingHealthRestriction_whenAddingRestriction_thenDoesNotDuplicateIt() {
        givenHealthRestrictionAlreadyAssigned();
        givenStudentExists();
        givenClassificationExists();

        whenAddHealthRestriction();

        thenClassificationWasAddedToStudent();
        thenStudentHasHealthRestrictionCount(1);
        thenStudentWasSaved();
    }

    private void givenDefaultHealthRestrictionRequest() {
        addRestrictionRequest = request(randomUUID(), randomUUID());
        student = student(addRestrictionRequest.studentId());
        classification = classification(addRestrictionRequest.classificationId());
    }

    private void givenHealthRestrictionAlreadyAssigned() {
        givenDefaultHealthRestrictionRequest();
        student.agregarRestriccionSalud(classification);
    }

    private void givenStudentExists() {
        when(alumnoRepository.findById(addRestrictionRequest.studentId()))
                .thenReturn(Optional.of(student));
    }

    private void givenStudentDoesNotExist() {
        when(alumnoRepository.findById(addRestrictionRequest.studentId()))
                .thenReturn(Optional.empty());
    }

    private void givenClassificationExists() {
        when(clasificacionRepository.findById(addRestrictionRequest.classificationId()))
                .thenReturn(Optional.of(classification));
    }

    private void givenClassificationDoesNotExist() {
        when(clasificacionRepository.findById(addRestrictionRequest.classificationId()))
                .thenReturn(Optional.empty());
    }

    private void whenAddHealthRestriction() {
        addHealthRestriction.execute(addRestrictionRequest);
    }

    private <T extends Throwable> void whenAddHealthRestrictionFails(Class<T> expectedException) {
        Executable execution = () -> addHealthRestriction.execute(addRestrictionRequest);
        thrownException = assertThrows(expectedException, execution);
    }

    private void thenClassificationWasAddedToStudent() {
        assertTrue(student.getRestriccionesSalud().contains(classification));
    }

    private void thenStudentHasHealthRestrictionCount(int expectedCount) {
        assertEquals(expectedCount, student.getRestriccionesSalud().size());
    }

    private void thenStudentWasSaved() {
        verify(alumnoRepository).save(student);
    }

    private void thenStudentWasNotSaved() {
        verify(alumnoRepository, never()).save(any());
    }

    private void thenClassificationWasNotSearched() {
        verify(clasificacionRepository, never()).findById(any());
    }

    private void thenErrorMessageIs(String expectedMessage) {
        assertEquals(expectedMessage, thrownException.getMessage());
    }

    private Alumno student(UUID studentId) {
        Alumno student = new Alumno();
        student.setId(studentId);
        return student;
    }

    private ClasificacionSalud classification(UUID classificationId) {
        ClasificacionSalud classification = new ClasificacionSalud();
        classification.setId(classificationId);
        return classification;
    }

    private AddHealthRestrictionRequest request(UUID studentId, UUID classificationId) {
        return new AddHealthRestrictionRequest(studentId, classificationId);
    }
}
