package com.recred.backend.application;

import com.recred.backend.domain.models.FranjaHoraria;

import java.time.LocalTime;
import java.util.UUID;

public record TimeSlotDetailsResponse(
        UUID id,
        UUID colegioId,
        String descripcion,
        LocalTime horaInicio,
        LocalTime horaFin,
        boolean activo,
        Integer cupoMaximo,
        Integer minutosCorte
) {

    public static TimeSlotDetailsResponse fromDomain(FranjaHoraria slot) {
        return new TimeSlotDetailsResponse(
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
}
