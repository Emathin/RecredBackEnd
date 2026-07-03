package com.recred.backend.application;

import java.time.LocalTime;
import java.util.UUID;

public record CreateTimeSlotRequest(
        UUID colegioId,
        String descripcion,
        LocalTime horaInicio,
        LocalTime horaFin,
        Integer cupoMaximo,
        Integer minutosCorte
) {
}
