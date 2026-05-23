package com.recred.backend.domain.models;

import com.recred.backend.control_parental.domain.enums.PeriodoPresupuesto;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PrediccionGasto(
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
        List<CategoriaConsumo> categoriasMasConsumidas
) {
}
