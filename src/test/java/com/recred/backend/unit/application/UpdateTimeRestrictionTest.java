package com.recred.backend.unit.application;

import com.recred.backend.application.UpdateTimeRestriction;
import com.recred.backend.application.UpdateTimeRestrictionRequest;
import com.recred.backend.application.UpdateTimeRestrictionResponse;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.ClasificacionSalud;
import com.recred.backend.domain.models.RestriccionHoraria;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.RestriccionHorariaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateTimeRestrictionTest {

    @Mock
    private RestriccionHorariaRepository repository;
    @Mock
    private AlumnoRepository studentRepository;

    @Captor
    private ArgumentCaptor<RestriccionHoraria> restrictionCaptor;

    @InjectMocks
    private UpdateTimeRestriction updateTimeRestriction;

    private UUID restrictionId;
    private UUID studentId;
    private UUID timeSlotId;
    private UUID categoryId;
    private UUID classificationId;
    private UpdateTimeRestrictionRequest updateRequest;
    private UpdateTimeRestrictionResponse updateResponse;

    @Test
    void givenExistingRestriction_whenUpdatingWithFilters_thenSavesNewFilters() {
        givenDefaultIds();
        givenRequestWithFilters();
        givenRestrictionExists();
        givenStudentExists(student());
        givenRestrictionCanBeSaved();

        whenUpdatingTimeRestriction();

        thenRestrictionWasSavedWithFilters();
        thenResponseIncludesFilters();
    }

    @Test
    void givenExistingRestriction_whenUpdatingWithNullFilters_thenClearsOptionalFilters() {
        givenDefaultIds();
        givenRequestWithoutOptionalFilters();
        givenRestrictionExists();
        givenStudentExists(student());
        givenRestrictionCanBeSaved();

        whenUpdatingTimeRestriction();

        thenRestrictionWasSavedWithoutOptionalFilters();
        thenResponseIncludesOnlyTimeSlot();
    }

    @Test
    void givenMissingRestriction_whenUpdatingRestriction_thenThrowsException() {
        givenDefaultIds();
        givenRequestWithoutOptionalFilters();
        givenRestrictionDoesNotExist();

        whenUpdateTimeRestrictionFails(RuntimeException.class);

        thenRestrictionWasNotSaved();
    }

    @Test
    void givenMissingStudent_whenUpdatingRestriction_thenThrowsException() {
        givenDefaultIds();
        givenRequestWithoutOptionalFilters();
        givenRestrictionExists();
        givenStudentDoesNotExist();

        whenUpdateTimeRestrictionFails(IllegalArgumentException.class);

        thenRestrictionWasNotSaved();
    }

    @Test
    void givenClassificationAlreadyAssignedGlobally_whenUpdatingRestriction_thenThrowsException() {
        givenDefaultIds();
        givenRequestWithClassification();
        givenRestrictionExists();
        givenStudentExists(studentWithGlobalClassification());

        whenUpdateTimeRestrictionFails(IllegalArgumentException.class);

        thenRestrictionWasNotSaved();
    }

    private void givenDefaultIds() {
        restrictionId = randomUUID();
        studentId = randomUUID();
        timeSlotId = randomUUID();
        categoryId = randomUUID();
        classificationId = randomUUID();
    }

    private void givenRequestWithFilters() {
        updateRequest = new UpdateTimeRestrictionRequest(
                restrictionId,
                timeSlotId,
                categoryId,
                classificationId);
    }

    private void givenRequestWithoutOptionalFilters() {
        updateRequest = new UpdateTimeRestrictionRequest(
                restrictionId,
                timeSlotId,
                null,
                null);
    }

    private void givenRequestWithClassification() {
        updateRequest = new UpdateTimeRestrictionRequest(
                restrictionId,
                timeSlotId,
                null,
                classificationId);
    }

    private void givenRestrictionExists() {
        when(repository.findById(restrictionId))
                .thenReturn(Optional.of(restriction(studentId)));
    }

    private void givenRestrictionDoesNotExist() {
        when(repository.findById(restrictionId)).thenReturn(Optional.empty());
    }

    private void givenStudentExists(Alumno student) {
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
    }

    private void givenStudentDoesNotExist() {
        when(studentRepository.findById(studentId)).thenReturn(Optional.empty());
    }

    private void givenRestrictionCanBeSaved() {
        when(repository.save(any(RestriccionHoraria.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void whenUpdatingTimeRestriction() {
        updateResponse = updateTimeRestriction.execute(updateRequest);
    }

    private <T extends Throwable> void whenUpdateTimeRestrictionFails(Class<T> exceptionType) {
        Executable execution = () -> updateTimeRestriction.execute(updateRequest);
        assertThrows(exceptionType, execution);
    }

    private void thenRestrictionWasSavedWithFilters() {
        verify(repository).save(restrictionCaptor.capture());
        RestriccionHoraria savedRestriction = restrictionCaptor.getValue();
        assertEquals(timeSlotId, savedRestriction.getFranjaHoraria().getId());
        assertEquals(categoryId, savedRestriction.getCategoria().getId());
        assertEquals(classificationId, savedRestriction.getClasificacionSalud().getId());
    }

    private void thenResponseIncludesFilters() {
        assertNotNull(updateResponse);
        assertEquals(timeSlotId, updateResponse.franjaHoraria().id());
        assertEquals(categoryId, updateResponse.categoria().id());
        assertEquals(classificationId, updateResponse.clasificacionSalud().id());
    }

    private void thenRestrictionWasSavedWithoutOptionalFilters() {
        verify(repository).save(restrictionCaptor.capture());
        RestriccionHoraria savedRestriction = restrictionCaptor.getValue();
        assertEquals(timeSlotId, savedRestriction.getFranjaHoraria().getId());
        assertNull(savedRestriction.getCategoria());
        assertNull(savedRestriction.getClasificacionSalud());
    }

    private void thenResponseIncludesOnlyTimeSlot() {
        assertNotNull(updateResponse);
        assertEquals(timeSlotId, updateResponse.franjaHoraria().id());
        assertNull(updateResponse.categoria());
        assertNull(updateResponse.clasificacionSalud());
    }

    private void thenRestrictionWasNotSaved() {
        verify(repository, never()).save(any());
    }

    private RestriccionHoraria restriction(UUID studentId) {
        RestriccionHoraria restriction = new RestriccionHoraria();
        restriction.setAlumnoId(studentId);
        return restriction;
    }

    private Alumno student() {
        return new Alumno();
    }

    private Alumno studentWithGlobalClassification() {
        ClasificacionSalud classification = new ClasificacionSalud();
        classification.setId(classificationId);

        Alumno student = new Alumno();
        student.setRestriccionesSalud(Set.of(classification));
        return student;
    }
}
