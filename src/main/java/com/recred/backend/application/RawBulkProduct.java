package com.recred.backend.application;

import java.math.BigDecimal;
import java.util.List;

public record RawBulkProduct(
        String nombre,
        String descripcion,
        BigDecimal precio,
        BigDecimal peso,
        boolean requierePreparacion,
        String categoriaNombre,
        Integer stockActual,
        List<String> saludEtiquetas,
        List<String> tipoEtiquetas
) {
}
