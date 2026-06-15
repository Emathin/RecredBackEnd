package com.recred.backend.infrastructure.web;

import com.recred.backend.application.SaveProductCommand;
import com.recred.backend.domain.models.TipoManejoInventario;

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
        TipoManejoInventario tipoManejoInventario,
        Integer cupoMaximoDiario,
        Set<UUID> clasificacionesSaludIds,
        Set<UUID> tiposIds
) {
    public SaveProductDTO(
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
        this(
                nombre,
                descripcion,
                precio,
                peso,
                requierePreparacion,
                categoriaId,
                nuevaCategoriaNombre,
                buffetId,
                stockActual,
                stockActual == null ? null : TipoManejoInventario.STOCK_EXACTO,
                null,
                clasificacionesSaludIds,
                tiposIds
        );
    }

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
                tiposIds,
                tipoManejoInventario,
                cupoMaximoDiario
        );
    }
}
