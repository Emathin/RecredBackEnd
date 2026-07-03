package com.recred.backend.application;

import java.time.LocalTime;
import java.util.UUID;

public record UpdateTimeSlotRequest(
        UUID colegioId,
        UUID slotId,
        String descripcion,
        LocalTime horaInicio,
        LocalTime horaFin,
        Integer cupoMaximo,
        Integer minutosCorte
) {
}
