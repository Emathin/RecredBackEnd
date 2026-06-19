package com.recred.backend.application;

import com.recred.backend.domain.models.TipoManejoInventario;
import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

public record UpdateProductCommand(
        UUID id,
        String nombre,
        String descripcion,
        BigDecimal precio,
        BigDecimal peso,
        Boolean requierePreparacion,
        UUID categoriaId,
        String nuevaCategoriaNombre,
        UUID buffetId,
        Integer stockActual,
        Set<UUID> clasificacionesSaludIds,
        Set<UUID> tiposIds,
        TipoManejoInventario tipoManejoInventario,
        Integer cupoMaximoDiario,
        String urlImagen
) {
    public UpdateProductCommand(
            UUID id,
            String nombre,
            String descripcion,
            BigDecimal precio,
            BigDecimal peso,
            Boolean requierePreparacion,
            UUID categoriaId,
            String nuevaCategoriaNombre,
            UUID buffetId,
            Integer stockActual,
            Set<UUID> clasificacionesSaludIds,
            Set<UUID> tiposIds
    ) {
        this(
                id,
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
                TipoManejoInventario.STOCK_EXACTO,
                null,
                null
        );
    }
}
