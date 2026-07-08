package com.recred.backend.application;

import com.recred.backend.domain.models.FranjaHoraria;

import java.time.LocalTime;
import java.util.UUID;

public record UpdatePickupSlotRulesResponse(
        UUID id,
        UUID colegioId,
        String descripcion,
        LocalTime horaInicio,
        LocalTime horaFin,
        Integer cupoMaximo,
        Integer minutosCorte
) {

    public static UpdatePickupSlotRulesResponse fromDomain(FranjaHoraria slot) {
        return new UpdatePickupSlotRulesResponse(
                slot.getId(),
                slot.getColegioId(),
                slot.getDescripcion(),
                slot.getHoraInicio(),
                slot.getHoraFin(),
                slot.getCupoMaximo(),
                slot.getMinutosCorte()
        );
    }
}
