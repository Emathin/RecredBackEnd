package com.recred.backend.domain.models;

import java.util.List;

public record AnalisisPrediccionGasto(
        String resumen,
        List<String> alertas,
        List<String> recomendaciones,
        String modelo
) {
}
