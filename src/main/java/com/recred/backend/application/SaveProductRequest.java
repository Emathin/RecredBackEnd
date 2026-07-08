package com.recred.backend.application;

import com.recred.backend.domain.models.TipoManejoInventario;
import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

public record SaveProductRequest(
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
        Set<UUID> tiposIds,
        TipoManejoInventario tipoManejoInventario,
        Integer cupoMaximoDiario,
        String urlImagen
) {
    public SaveProductRequest(
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
                clasificacionesSaludIds,
                tiposIds,
                null,
                null,
                null
        );
    }
}
