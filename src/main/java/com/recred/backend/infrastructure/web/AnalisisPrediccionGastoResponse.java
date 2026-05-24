package com.recred.backend.infrastructure.web;

import com.recred.backend.domain.models.AnalisisPrediccionGasto;
import java.util.List;

public record AnalisisPrediccionGastoResponse(
        String resumen,
        List<String> alertas,
        List<String> recomendaciones,
        String modelo
) {

    public static AnalisisPrediccionGastoResponse fromDomain(
            AnalisisPrediccionGasto analisis
    ) {
        return new AnalisisPrediccionGastoResponse(
                analisis.resumen(),
                analisis.alertas(),
                analisis.recomendaciones(),
                analisis.modelo()
        );
    }
}
