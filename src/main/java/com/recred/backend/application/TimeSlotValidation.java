package com.recred.backend.application;

import com.recred.backend.domain.exceptions.InvalidTimeSlotException;
import com.recred.backend.domain.exceptions.TimeSlotOverlapException;
import com.recred.backend.domain.models.FranjaHoraria;
import com.recred.backend.domain.repositories.FranjaHorariaRepository;

import java.time.LocalTime;
import java.util.Objects;
import java.util.UUID;

final class TimeSlotValidation {

    private TimeSlotValidation() {
    }

    static void validateIds(UUID colegioId, UUID slotId) {
        validateSchoolId(colegioId);
        if (slotId == null) {
            throw new InvalidTimeSlotException("slotId es requerido");
        }
    }

    static void validateSchoolId(UUID colegioId) {
        if (colegioId == null) {
            throw new InvalidTimeSlotException("colegioId es requerido");
        }
    }

    static String validatedDescription(String descripcion) {
        if (descripcion == null || descripcion.isBlank()) {
            throw new InvalidTimeSlotException("La descripcion es requerida");
        }

        return descripcion.trim();
    }

    static void validateTimeRange(LocalTime horaInicio, LocalTime horaFin) {
        if (horaInicio == null) {
            throw new InvalidTimeSlotException("horaInicio es requerida");
        }

        if (horaFin == null) {
            throw new InvalidTimeSlotException("horaFin es requerida");
        }

        if (!horaInicio.isBefore(horaFin)) {
            throw new InvalidTimeSlotException("horaInicio debe ser anterior a horaFin");
        }
    }

    static Integer validatedCapacity(Integer cupoMaximo) {
        if (cupoMaximo != null && cupoMaximo < 0) {
            throw new InvalidTimeSlotException("cupoMaximo no puede ser negativo");
        }

        return cupoMaximo;
    }

    static int validatedCutoff(Integer minutosCorte) {
        if (minutosCorte == null) {
            return 0;
        }

        if (minutosCorte < 0) {
            throw new InvalidTimeSlotException("minutosCorte no puede ser negativo");
        }

        return minutosCorte;
    }

    static void requireNoActiveOverlap(
            FranjaHorariaRepository repository,
            UUID colegioId,
            UUID excludedId,
            LocalTime horaInicio,
            LocalTime horaFin
    ) {
        if (repository.existsActiveOverlap(colegioId, excludedId, horaInicio, horaFin)) {
            throw new TimeSlotOverlapException(colegioId);
        }
    }

    static boolean hasStructuralChanges(FranjaHoraria slot, String descripcion,
                                        LocalTime horaInicio, LocalTime horaFin) {
        return !Objects.equals(slot.getDescripcion(), descripcion)
                || !Objects.equals(slot.getHoraInicio(), horaInicio)
                || !Objects.equals(slot.getHoraFin(), horaFin);
    }
}
