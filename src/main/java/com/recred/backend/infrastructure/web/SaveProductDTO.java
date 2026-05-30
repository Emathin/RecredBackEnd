package com.recred.backend.infrastructure.web;

import com.recred.backend.application.SaveProductCommand;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

public record SaveProductDTO(
        String nombre,
        String descripcion,
        BigDecimal precio,
        BigDecimal peso,
        boolean requierePreparacion,
        UUID categoriaId,
        String nuevaCategoriaNombre,
        UUID buffetId,
        Integer stockActual,
        Set<UUID> clasificacionesSaludIds,
        Set<UUID> tiposIds
) {
    public SaveProductCommand toCommand() {
        return new SaveProductCommand(
                nombre,
                descripcion,
                precio,
                peso,
                requierePreparacion,
                categoriaId,
                nuevaCategoriaNombre,
                buffetId,
                stockActual,
                clasificacionesSaludIds,
                tiposIds
        );
    }
}
