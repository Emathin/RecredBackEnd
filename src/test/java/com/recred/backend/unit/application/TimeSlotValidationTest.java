package com.recred.backend.application;

import com.recred.backend.domain.exceptions.InvalidTimeSlotException;
import com.recred.backend.domain.exceptions.TimeSlotOverlapException;
import com.recred.backend.domain.models.FranjaHoraria;
import com.recred.backend.domain.repositories.FranjaHorariaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.time.LocalTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TimeSlotValidationTest {

    private static final UUID COLEGIO_ID = UUID.randomUUID();
    private static final UUID SLOT_ID = UUID.randomUUID();
    private static final LocalTime INICIO = LocalTime.of(10, 0);
    private static final LocalTime FIN = LocalTime.of(10, 30);

    private final FranjaHorariaRepository repository = mock(FranjaHorariaRepository.class);

    @Test
    void shouldRejectMissingSchoolId() {
        Executable validation = () -> TimeSlotValidation.validateIds(null, SLOT_ID);

        thenThrowsInvalidTimeSlot(validation, "colegioId es requerido");
    }

    @Test
    void shouldRejectMissingSlotId() {
        Executable validation = () -> TimeSlotValidation.validateIds(COLEGIO_ID, null);

        thenThrowsInvalidTimeSlot(validation, "slotId es requerido");
    }

    @Test
    void shouldNormalizeValidDescription() {
        String result = TimeSlotValidation.validatedDescription("  Recreo  ");

        thenReturns("Recreo", result);
    }

    @Test
    void shouldRejectBlankDescription() {
        Executable validation = () -> TimeSlotValidation.validatedDescription(" ");

        thenThrowsInvalidTimeSlot(validation, "La descripcion es requerida");
    }

    @Test
    void shouldAcceptAndNormalizeCapacity() {
        Integer result = TimeSlotValidation.validatedCapacity(10);

        thenReturns(10, result);
    }

    @Test
    void shouldRejectNegativeCapacity() {
        Executable validation = () -> TimeSlotValidation.validatedCapacity(-1);

        thenThrowsInvalidTimeSlot(validation, "cupoMaximo no puede ser negativo");
    }

    @Test
    void shouldAcceptAndNormalizeCutoff() {
        int resultNotNull = TimeSlotValidation.validatedCutoff(15);
        int resultNull = TimeSlotValidation.validatedCutoff(null);

        thenReturns(15, resultNotNull);
        thenReturns(0, resultNull);
    }

    @Test
    void shouldRejectNegativeCutoff() {
        Executable validation = () -> TimeSlotValidation.validatedCutoff(-1);

        thenThrowsInvalidTimeSlot(validation, "minutosCorte no puede ser negativo");
    }

    @Test
    void shouldRejectInvalidTimeRanges() {
        Executable noStart = () -> TimeSlotValidation.validateTimeRange(null, LocalTime.NOON);
        Executable noEnd = () -> TimeSlotValidation.validateTimeRange(LocalTime.NOON, null);
        Executable inverted = () -> TimeSlotValidation.validateTimeRange(LocalTime.NOON, LocalTime.NOON);

        thenThrowsInvalidTimeSlot(noStart, "horaInicio es requerida");
        thenThrowsInvalidTimeSlot(noEnd, "horaFin es requerida");
        thenThrowsInvalidTimeSlot(inverted, "horaInicio debe ser anterior a horaFin");
    }

    @Test
    void shouldNotThrowWhenNoOverlapExists() {
        givenNoOverlap();

        Executable validation = () -> TimeSlotValidation.requireNoActiveOverlap(
                repository, COLEGIO_ID, SLOT_ID, INICIO, FIN);

        thenValidationPasses(validation);
    }

    @Test
    void shouldThrowWhenOverlapExists() {
        givenOverlapExists();

        Executable validation = () -> TimeSlotValidation.requireNoActiveOverlap(
                repository, COLEGIO_ID, SLOT_ID, INICIO, FIN);

        thenThrowsOverlapException(validation);
    }

    @Test
    void shouldDetectNoStructuralChanges() {
        FranjaHoraria slot = slotEntity("Recreo", INICIO, FIN);

        boolean changed = TimeSlotValidation.hasStructuralChanges(slot, "Recreo", INICIO, FIN);

        thenHasNoStructuralChanges(changed);
    }

    @Test
    void shouldDetectStructuralChangesInDescription() {
        FranjaHoraria slot = slotEntity("Recreo", INICIO, FIN);

        boolean changed = TimeSlotValidation.hasStructuralChanges(slot, "Almuerzo", INICIO, FIN);

        thenHasStructuralChanges(changed);
    }

    @Test
    void shouldDetectStructuralChangesInTimeRange() {
        FranjaHoraria slot = slotEntity("Recreo", INICIO, FIN);

        boolean changed = TimeSlotValidation.hasStructuralChanges(
                slot, "Recreo", LocalTime.of(10, 5), FIN);

        thenHasStructuralChanges(changed);
    }

    // --- Helpers Privados ---

    private void givenNoOverlap() {
        when(repository.existsActiveOverlap(COLEGIO_ID, SLOT_ID, INICIO, FIN))
                .thenReturn(false);
    }

    private void givenOverlapExists() {
        when(repository.existsActiveOverlap(COLEGIO_ID, SLOT_ID, INICIO, FIN))
                .thenReturn(true);
    }

    private FranjaHoraria slotEntity(String descripcion, LocalTime start, LocalTime end) {
        FranjaHoraria slot = new FranjaHoraria();
        slot.setDescripcion(descripcion);
        slot.setHoraInicio(start);
        slot.setHoraFin(end);
        return slot;
    }

    private void thenThrowsInvalidTimeSlot(Executable execution, String expectedMessage) {
        InvalidTimeSlotException ex = assertThrows(InvalidTimeSlotException.class, execution);
        assertEquals(expectedMessage, ex.getMessage());
    }

    private void thenThrowsOverlapException(Executable execution) {
        assertThrows(TimeSlotOverlapException.class, execution);
    }

    private void thenValidationPasses(Executable execution) {
        assertDoesNotThrow(execution);
    }

    private void thenReturns(Object expected, Object actual) {
        assertEquals(expected, actual);
    }

    private void thenHasStructuralChanges(boolean changed) {
        assertTrue(changed);
    }

    private void thenHasNoStructuralChanges(boolean changed) {
        assertFalse(changed);
    }
}
