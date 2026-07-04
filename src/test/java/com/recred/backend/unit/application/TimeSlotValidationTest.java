package com.recred.backend.application;

import com.recred.backend.domain.exceptions.InvalidTimeSlotException;
import com.recred.backend.domain.exceptions.TimeSlotOverlapException;
import com.recred.backend.domain.models.FranjaHoraria;
import com.recred.backend.domain.repositories.FranjaHorariaRepository;
import java.time.LocalTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TimeSlotValidationTest {

    @Test
    void validateIdsShouldRejectMissingIds() {
        UUID schoolId = UUID.randomUUID();

        assertEquals("colegioId es requerido",
                assertThrows(InvalidTimeSlotException.class,
                        () -> TimeSlotValidation.validateIds(null, UUID.randomUUID()))
                        .getMessage());
        assertEquals("slotId es requerido",
                assertThrows(InvalidTimeSlotException.class,
                        () -> TimeSlotValidation.validateIds(schoolId, null))
                        .getMessage());
    }

    @Test
    void validatorsShouldNormalizeAndRejectInvalidValues() {
        assertEquals("Recreo",
                TimeSlotValidation.validatedDescription("  Recreo  "));
        assertEquals(10, TimeSlotValidation.validatedCapacity(10));
        assertEquals(0, TimeSlotValidation.validatedCutoff(null));
        assertEquals(15, TimeSlotValidation.validatedCutoff(15));

        assertThrows(InvalidTimeSlotException.class,
                () -> TimeSlotValidation.validatedDescription(" "));
        assertThrows(InvalidTimeSlotException.class,
                () -> TimeSlotValidation.validateTimeRange(null, LocalTime.NOON));
        assertThrows(InvalidTimeSlotException.class,
                () -> TimeSlotValidation.validateTimeRange(LocalTime.NOON, null));
        assertThrows(InvalidTimeSlotException.class,
                () -> TimeSlotValidation.validateTimeRange(LocalTime.NOON, LocalTime.NOON));
        assertThrows(InvalidTimeSlotException.class,
                () -> TimeSlotValidation.validatedCapacity(-1));
        assertThrows(InvalidTimeSlotException.class,
                () -> TimeSlotValidation.validatedCutoff(-1));
    }

    @Test
    void requireNoActiveOverlapShouldThrowOnlyWhenRepositoryFindsOverlap() {
        FranjaHorariaRepository repository = mock(FranjaHorariaRepository.class);
        UUID schoolId = UUID.randomUUID();
        UUID excludedId = UUID.randomUUID();
        LocalTime start = LocalTime.of(10, 0);
        LocalTime end = LocalTime.of(10, 30);
        when(repository.existsActiveOverlap(schoolId, excludedId, start, end))
                .thenReturn(false, true);

        assertDoesNotThrow(() -> TimeSlotValidation.requireNoActiveOverlap(
                repository,
                schoolId,
                excludedId,
                start,
                end
        ));
        assertThrows(TimeSlotOverlapException.class,
                () -> TimeSlotValidation.requireNoActiveOverlap(
                        repository,
                        schoolId,
                        excludedId,
                        start,
                        end
                ));
    }

    @Test
    void hasStructuralChangesShouldCompareDescriptionAndTimeRange() {
        FranjaHoraria slot = new FranjaHoraria();
        slot.setDescripcion("Recreo");
        slot.setHoraInicio(LocalTime.of(10, 0));
        slot.setHoraFin(LocalTime.of(10, 30));

        assertFalse(TimeSlotValidation.hasStructuralChanges(
                slot,
                "Recreo",
                LocalTime.of(10, 0),
                LocalTime.of(10, 30)
        ));
        assertTrue(TimeSlotValidation.hasStructuralChanges(
                slot,
                "Almuerzo",
                LocalTime.of(10, 0),
                LocalTime.of(10, 30)
        ));
        assertTrue(TimeSlotValidation.hasStructuralChanges(
                slot,
                "Recreo",
                LocalTime.of(10, 5),
                LocalTime.of(10, 30)
        ));
    }
}
