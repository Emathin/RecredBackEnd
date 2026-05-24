package com.recred.backend.domain.services;

import com.recred.backend.domain.models.PeriodoPresupuesto;
import com.recred.backend.domain.models.CategoriaConsumo;
import com.recred.backend.domain.models.GastoCompra;
import com.recred.backend.domain.models.PrediccionGasto;
import com.recred.backend.domain.models.PreferenciaPeriodoGasto;
import com.recred.backend.domain.models.RangoFecha;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class PrediccionGastoCalculator {

    private static final int ESCALA_MONTO = 2;
    private static final int ESCALA_CONFIANZA = 4;
    private static final BigDecimal CIEN = BigDecimal.valueOf(100);
    private static final BigDecimal CONFIANZA_SIN_DATOS = BigDecimal.valueOf(0.10);
    private static final BigDecimal CONFIANZA_BASE = BigDecimal.valueOf(0.35);
    private static final BigDecimal CONFIANZA_MAXIMA = BigDecimal.valueOf(0.95);

    public PrediccionGasto calcular(
            UUID alumnoId,
            PeriodoPresupuesto periodo,
            LocalDate fechaCalculo,
            RangoFecha rangoActual,
            RangoFecha rangoHistorico,
            List<GastoCompra> gastosPeriodoActual,
            List<GastoCompra> gastosHistoricos,
            Optional<PreferenciaPeriodoGasto> preferencia,
            List<CategoriaConsumo> categoriasMasConsumidas
    ) {
        BigDecimal gastoActual = sumar(gastosPeriodoActual);
        int diasRestantes = contarDiasOperativos(fechaCalculo.plusDays(1), rangoActual.fechaFin());
        int diasHistoricos = contarDiasOperativos(
                rangoHistorico.fechaInicio(),
                rangoHistorico.fechaFin()
        );

        BigDecimal promedioDiario = calcularPromedioDiario(
                gastosHistoricos,
                diasHistoricos,
                gastoActual,
                contarDiasOperativos(rangoActual.fechaInicio(), fechaCalculo)
        );
        BigDecimal gastoPredicho = gastoActual.add(promedioDiario.multiply(
                BigDecimal.valueOf(diasRestantes)));
        BigDecimal montoLimite = preferencia.map(PreferenciaPeriodoGasto::montoLimite).orElse(null);
        BigDecimal porcentajePresupuesto = calcularPorcentaje(gastoPredicho, montoLimite);

        return new PrediccionGasto(
                alumnoId,
                periodo,
                fechaCalculo,
                rangoActual.fechaInicio(),
                rangoActual.fechaFin(),
                escalarMonto(gastoActual),
                escalarMonto(gastoPredicho),
                escalarMonto(promedioDiario),
                escalarMonto(montoLimite),
                porcentajePresupuesto,
                calcularConfianza(gastosHistoricos, gastosPeriodoActual),
                diasHistoricos,
                diasRestantes,
                categoriasMasConsumidas
        );
    }

    public int contarDiasOperativos(LocalDate desde, LocalDate hasta) {
        if (desde.isAfter(hasta)) {
            return 0;
        }

        int dias = 0;
        LocalDate fecha = desde;
        while (!fecha.isAfter(hasta)) {
            if (esDiaOperativo(fecha)) {
                dias++;
            }
            fecha = fecha.plusDays(1);
        }
        return dias;
    }

    private BigDecimal calcularPromedioDiario(
            List<GastoCompra> gastosHistoricos,
            int diasHistoricos,
            BigDecimal gastoActual,
            int diasActuales
    ) {
        BigDecimal gastoHistorico = sumar(gastosHistoricos);
        if (diasHistoricos > 0 && gastoHistorico.compareTo(BigDecimal.ZERO) > 0) {
            return dividir(gastoHistorico, diasHistoricos);
        }
        if (diasActuales > 0 && gastoActual.compareTo(BigDecimal.ZERO) > 0) {
            return dividir(gastoActual, diasActuales);
        }
        return BigDecimal.ZERO.setScale(ESCALA_MONTO, RoundingMode.HALF_UP);
    }

    private BigDecimal sumar(List<GastoCompra> gastos) {
        return gastos.stream()
                .map(GastoCompra::monto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal dividir(BigDecimal monto, int divisor) {
        return monto.divide(BigDecimal.valueOf(divisor), ESCALA_MONTO, RoundingMode.HALF_UP);
    }

    private BigDecimal calcularPorcentaje(BigDecimal gastoPredicho, BigDecimal montoLimite) {
        if (montoLimite == null || montoLimite.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return gastoPredicho
                .multiply(CIEN)
                .divide(montoLimite, ESCALA_MONTO, RoundingMode.HALF_UP);
    }

    private BigDecimal calcularConfianza(
            List<GastoCompra> gastosHistoricos,
            List<GastoCompra> gastosPeriodoActual
    ) {
        if (gastosHistoricos.isEmpty() && gastosPeriodoActual.isEmpty()) {
            return CONFIANZA_SIN_DATOS.setScale(ESCALA_CONFIANZA, RoundingMode.HALF_UP);
        }

        BigDecimal incremento = BigDecimal.valueOf(gastosHistoricos.size())
                .divide(BigDecimal.valueOf(100), ESCALA_CONFIANZA, RoundingMode.HALF_UP);
        BigDecimal confianza = CONFIANZA_BASE.add(incremento);
        if (confianza.compareTo(CONFIANZA_MAXIMA) > 0) {
            return CONFIANZA_MAXIMA.setScale(ESCALA_CONFIANZA, RoundingMode.HALF_UP);
        }
        return confianza.setScale(ESCALA_CONFIANZA, RoundingMode.HALF_UP);
    }

    private BigDecimal escalarMonto(BigDecimal monto) {
        if (monto == null) {
            return null;
        }
        return monto.setScale(ESCALA_MONTO, RoundingMode.HALF_UP);
    }

    private boolean esDiaOperativo(LocalDate fecha) {
        return fecha.getDayOfWeek() != DayOfWeek.SATURDAY
                && fecha.getDayOfWeek() != DayOfWeek.SUNDAY;
    }
}
