package com.recred.backend.application;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

public record BulkProductData(
        String nombre,
        String descripcion,
        BigDecimal precio,
        BigDecimal peso,
        boolean requierePreparacion,
        UUID categoriaId,
        String nuevaCategoriaNombre,
        Integer stockActual,
        Set<UUID> saludEtiquetasIds,
        Set<UUID> tipoEtiquetasIds
) {
}
