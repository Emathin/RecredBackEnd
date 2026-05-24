package com.recred.backend.infrastructure.web;

import com.recred.backend.domain.models.PeriodoPresupuesto;
import com.recred.backend.domain.models.PrediccionGasto;
import com.recred.backend.domain.models.PrediccionGastoResultado;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PrediccionGastoResponse(
        UUID alumnoId,
        PeriodoPresupuesto periodo,
        LocalDate fechaCalculo,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        BigDecimal gastoActual,
        BigDecimal gastoPredicho,
        BigDecimal promedioGastoDiario,
        BigDecimal montoLimite,
        BigDecimal porcentajePresupuesto,
        BigDecimal confianza,
        int diasHistoricosUsados,
        int diasRestantes,
        List<CategoriaConsumoResponse> categoriasMasConsumidas,
        AnalisisPrediccionGastoResponse analisisIa
) {

    public static PrediccionGastoResponse fromDomain(PrediccionGastoResultado resultado) {
        PrediccionGasto prediccionGasto = resultado.prediccionGasto();
        return new PrediccionGastoResponse(
                prediccionGasto.alumnoId(),
                prediccionGasto.periodo(),
                prediccionGasto.fechaCalculo(),
                prediccionGasto.fechaInicio(),
                prediccionGasto.fechaFin(),
                prediccionGasto.gastoActual(),
                prediccionGasto.gastoPredicho(),
                prediccionGasto.promedioGastoDiario(),
                prediccionGasto.montoLimite(),
                prediccionGasto.porcentajePresupuesto(),
                prediccionGasto.confianza(),
                prediccionGasto.diasHistoricosUsados(),
                prediccionGasto.diasRestantes(),
                prediccionGasto.categoriasMasConsumidas().stream()
                        .map(CategoriaConsumoResponse::fromDomain)
                        .toList(),
                resultado.analisisIa()
                        .map(AnalisisPrediccionGastoResponse::fromDomain)
                        .orElse(null)
        );
    }
}
