package com.recred.backend.application;

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
        Set<UUID> tiposIds
) {
}
