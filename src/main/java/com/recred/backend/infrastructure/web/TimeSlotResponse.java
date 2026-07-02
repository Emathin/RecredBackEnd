package com.recred.backend.infrastructure.web;


import com.recred.backend.application.GetTimeSlotsBySchoolResponse;

import java.time.LocalTime;
import java.util.UUID;

public record TimeSlotResponse(
        UUID id,
        UUID colegioId,
        String descripcion,
        LocalTime horaInicio,
        LocalTime horaFin,
        boolean activo,
        Integer cupoMaximo,
        Integer minutosCorte
) {


    public static TimeSlotResponse fromUseCaseResponse(GetTimeSlotsBySchoolResponse slot) {
        return new TimeSlotResponse(
                slot.id(),
                slot.colegioId(),
                slot.descripcion(),
                slot.horaInicio(),
                slot.horaFin(),
                slot.activo(),
                slot.cupoMaximo(),
                slot.minutosCorte()
        );
    }
}
