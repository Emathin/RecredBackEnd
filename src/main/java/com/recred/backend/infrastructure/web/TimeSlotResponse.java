package com.recred.backend.infrastructure.web;

import com.recred.backend.domain.models.FranjaHoraria;
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
    public static TimeSlotResponse fromDomain(FranjaHoraria slot) {
        return new TimeSlotResponse(
                slot.getId(),
                slot.getColegioId(),
                slot.getDescripcion(),
                slot.getHoraInicio(),
                slot.getHoraFin(),
                slot.isActivo(),
                slot.getCupoMaximo(),
                slot.getMinutosCorte()
        );
    }

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
