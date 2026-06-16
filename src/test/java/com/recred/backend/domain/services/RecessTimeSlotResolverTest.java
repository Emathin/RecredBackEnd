package com.recred.backend.domain.services;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Colegio;
import com.recred.backend.domain.models.FranjaHoraria;
import com.recred.backend.domain.models.Grado;
import com.recred.backend.domain.models.RecessTime;
import com.recred.backend.domain.repositories.FranjaHorariaRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RecessTimeSlotResolverTest {

    @Test
    void resolve_ShouldMatchByDescription() {
        UUID schoolId = UUID.randomUUID();
        FranjaHoraria first = slot("Primer recreo", 9);
        FranjaHoraria second = slot("Segundo recreo", 11);
        FranjaHorariaRepository repository = mock(FranjaHorariaRepository.class);
        RecessTimeSlotResolver resolver = new RecessTimeSlotResolver(repository);

        when(repository.findAllByColegioId(schoolId)).thenReturn(List.of(first, second));

        FranjaHoraria result = resolver.resolve(student(schoolId), RecessTime.SECOND_RECESS);

        assertEquals(second.getId(), result.getId());
    }

    @Test
    void resolve_WhenDescriptionDoesNotMatch_ShouldUsePosition() {
        UUID schoolId = UUID.randomUUID();
        FranjaHoraria first = slot("Turno 1", 9);
        FranjaHoraria second = slot("Turno 2", 11);
        FranjaHorariaRepository repository = mock(FranjaHorariaRepository.class);
        RecessTimeSlotResolver resolver = new RecessTimeSlotResolver(repository);

        when(repository.findAllByColegioId(schoolId)).thenReturn(List.of(second, first));

        FranjaHoraria result = resolver.resolve(student(schoolId), RecessTime.FIRST_RECESS);

        assertEquals(first.getId(), result.getId());
    }

    @Test
    void resolve_WhenDescriptionDoesNotMatch_ShouldUseSecondAndNoonPositions() {
        UUID schoolId = UUID.randomUUID();
        FranjaHoraria first = slot("Turno 1", 9);
        FranjaHoraria second = slot("Turno 2", 11);
        FranjaHoraria noon = slot("Turno 3", 13);
        FranjaHorariaRepository repository = mock(FranjaHorariaRepository.class);
        RecessTimeSlotResolver resolver = new RecessTimeSlotResolver(repository);

        when(repository.findAllByColegioId(schoolId)).thenReturn(List.of(noon, second, first));

        assertEquals(second.getId(),
                resolver.resolve(student(schoolId), RecessTime.SECOND_RECESS).getId());
        assertEquals(noon.getId(),
                resolver.resolve(student(schoolId), RecessTime.NOON).getId());
    }

    @Test
    void resolve_ShouldMatchNoonAndAfterHoursDescriptions() {
        UUID schoolId = UUID.randomUUID();
        FranjaHoraria noon = slot("Almuerzo mediodia", 12);
        FranjaHoraria afterHours = slot("Salida final", 16);
        FranjaHorariaRepository repository = mock(FranjaHorariaRepository.class);
        RecessTimeSlotResolver resolver = new RecessTimeSlotResolver(repository);

        when(repository.findAllByColegioId(schoolId)).thenReturn(List.of(afterHours, noon));

        assertEquals(noon.getId(), resolver.resolve(student(schoolId), RecessTime.NOON).getId());
        assertEquals(afterHours.getId(),
                resolver.resolve(student(schoolId), RecessTime.AFTER_HOURS).getId());
    }

    @Test
    void resolve_WhenDescriptionIsNull_ShouldUsePositionFallback() {
        UUID schoolId = UUID.randomUUID();
        FranjaHoraria first = slot(null, 9);
        FranjaHorariaRepository repository = mock(FranjaHorariaRepository.class);
        RecessTimeSlotResolver resolver = new RecessTimeSlotResolver(repository);

        when(repository.findAllByColegioId(schoolId)).thenReturn(List.of(first));

        FranjaHoraria result = resolver.resolve(student(schoolId), RecessTime.FIRST_RECESS);

        assertEquals(first.getId(), result.getId());
    }

    @Test
    void resolve_WhenStudentSchoolIsMissing_ShouldThrowException() {
        FranjaHorariaRepository repository = mock(FranjaHorariaRepository.class);
        RecessTimeSlotResolver resolver = new RecessTimeSlotResolver(repository);
        Alumno student = new Alumno();

        assertThrows(RuntimeException.class,
                () -> resolver.resolve(student, RecessTime.FIRST_RECESS));
    }

    @Test
    void resolve_WhenThereIsNoSlotForRequestedPosition_ShouldThrowException() {
        UUID schoolId = UUID.randomUUID();
        FranjaHorariaRepository repository = mock(FranjaHorariaRepository.class);
        RecessTimeSlotResolver resolver = new RecessTimeSlotResolver(repository);

        when(repository.findAllByColegioId(schoolId)).thenReturn(List.of(slot("Turno 1", 9)));

        assertThrows(RuntimeException.class,
                () -> resolver.resolve(student(schoolId), RecessTime.AFTER_HOURS));
    }

    private Alumno student(UUID schoolId) {
        Colegio school = new Colegio();
        school.setId(schoolId);
        Grado grade = new Grado();
        grade.setColegio(school);
        Alumno student = new Alumno();
        student.setId(UUID.randomUUID());
        student.setGrado(grade);
        return student;
    }

    private FranjaHoraria slot(String description, int hour) {
        FranjaHoraria slot = new FranjaHoraria();
        slot.setId(UUID.randomUUID());
        slot.setDescripcion(description);
        slot.setHoraInicio(LocalTime.of(hour, 0));
        slot.setHoraFin(LocalTime.of(hour, 20));
        return slot;
    }
}
