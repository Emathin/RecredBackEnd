package com.recred.backend.domain.services;

import com.recred.backend.domain.models.PeriodoPresupuesto;
import com.recred.backend.domain.models.CategoriaConsumo;
import com.recred.backend.domain.models.GastoCompra;
import com.recred.backend.domain.models.PrediccionGasto;
import com.recred.backend.domain.models.PreferenciaPeriodoGasto;
import com.recred.backend.domain.models.RangoFecha;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PrediccionGastoCalculatorTest {

    private final PrediccionGastoCalculator calculator = new PrediccionGastoCalculator();

    @Test
    void calcularPrediccionConHistorialPresupuestoYCategorias() {
        UUID alumnoId = UUID.randomUUID();
        LocalDate fechaCalculo = LocalDate.of(2026, 5, 8);
        RangoFecha rangoActual = new RangoFecha(
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 15)
        );
        RangoFecha rangoHistorico = new RangoFecha(
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 10)
        );

        PrediccionGasto prediccion = calculator.calcular(
                alumnoId,
                PeriodoPresupuesto.QUINCENAL,
                fechaCalculo,
                rangoActual,
                rangoHistorico,
                List.of(gasto("2026-05-02T10:00:00", "30.00")),
                List.of(
                        gasto("2026-04-01T10:00:00", "40.00"),
                        gasto("2026-04-03T10:00:00", "40.00")
                ),
                Optional.of(new PreferenciaPeriodoGasto(
                        PeriodoPresupuesto.QUINCENAL,
                        new BigDecimal("100.00")
                )),
                List.of(new CategoriaConsumo("Bebidas", new BigDecimal("30.00")))
        );

        assertEquals(alumnoId, prediccion.alumnoId());
        assertEquals(PeriodoPresupuesto.QUINCENAL, prediccion.periodo());
        assertEquals(new BigDecimal("30.00"), prediccion.gastoActual());
        assertEquals(new BigDecimal("80.00"), prediccion.gastoPredicho());
        assertEquals(new BigDecimal("10.00"), prediccion.promedioGastoDiario());
        assertEquals(new BigDecimal("100.00"), prediccion.montoLimite());
        assertEquals(new BigDecimal("80.00"), prediccion.porcentajePresupuesto());
        assertEquals(new BigDecimal("0.3700"), prediccion.confianza());
        assertEquals(8, prediccion.diasHistoricosUsados());
        assertEquals(5, prediccion.diasRestantes());
        assertEquals("Bebidas", prediccion.categoriasMasConsumidas().get(0).descripcion());
    }

    @Test
    void usarPromedioActualCuandoNoHayHistorial() {
        PrediccionGasto prediccion = calculator.calcular(
                UUID.randomUUID(),
                PeriodoPresupuesto.QUINCENAL,
                LocalDate.of(2026, 5, 8),
                new RangoFecha(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 15)),
                new RangoFecha(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 10)),
                List.of(gasto("2026-05-08T10:00:00", "90.00")),
                List.of(),
                Optional.empty(),
                List.of()
        );

        assertEquals(new BigDecimal("15.00"), prediccion.promedioGastoDiario());
        assertEquals(new BigDecimal("165.00"), prediccion.gastoPredicho());
        assertNull(prediccion.montoLimite());
        assertNull(prediccion.porcentajePresupuesto());
        assertEquals(new BigDecimal("0.3500"), prediccion.confianza());
    }

    @Test
    void devolverCeroYBajaConfianzaCuandoNoHayDatos() {
        PrediccionGasto prediccion = calculator.calcular(
                UUID.randomUUID(),
                PeriodoPresupuesto.MENSUAL,
                LocalDate.of(2026, 5, 8),
                new RangoFecha(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31)),
                new RangoFecha(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 10)),
                List.of(),
                List.of(),
                Optional.empty(),
                List.of()
        );

        assertEquals(new BigDecimal("0.00"), prediccion.gastoActual());
        assertEquals(new BigDecimal("0.00"), prediccion.gastoPredicho());
        assertEquals(new BigDecimal("0.00"), prediccion.promedioGastoDiario());
        assertEquals(new BigDecimal("0.1000"), prediccion.confianza());
    }

    @Test
    void limitarConfianzaMaxima() {
        List<GastoCompra> historicos = new ArrayList<>();
        for (int i = 0; i < 80; i++) {
            historicos.add(gasto("2026-04-01T10:00:00", "1.00"));
        }

        PrediccionGasto prediccion = calculator.calcular(
                UUID.randomUUID(),
                PeriodoPresupuesto.MENSUAL,
                LocalDate.of(2026, 5, 8),
                new RangoFecha(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31)),
                new RangoFecha(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30)),
                List.of(),
                historicos,
                Optional.empty(),
                List.of()
        );

        assertEquals(new BigDecimal("0.9500"), prediccion.confianza());
    }

    @Test
    void noContarDiasOperativosCuandoRangoEstaInvertido() {
        int dias = calculator.contarDiasOperativos(
                LocalDate.of(2026, 5, 10),
                LocalDate.of(2026, 5, 8)
        );

        assertEquals(0, dias);
    }

    private GastoCompra gasto(String fechaHora, String monto) {
        return new GastoCompra(LocalDateTime.parse(fechaHora), new BigDecimal(monto));
    }
}
