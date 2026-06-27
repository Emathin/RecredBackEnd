package com.recred.backend.application;

import java.time.LocalTime;
import java.util.UUID;

public record GetTimeSlotsBySchoolResponse(
        UUID id,
        UUID colegioId,
        String descripcion,
        LocalTime horaInicio,
        LocalTime horaFin,
        boolean activo,
        Integer cupoMaximo,
        Integer minutosCorte
) {}
