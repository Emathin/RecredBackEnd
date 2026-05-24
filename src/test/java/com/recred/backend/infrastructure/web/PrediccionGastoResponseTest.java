package com.recred.backend.infrastructure.web;

import com.recred.backend.domain.models.PeriodoPresupuesto;
import com.recred.backend.domain.models.AnalisisPrediccionGasto;
import com.recred.backend.domain.models.CategoriaConsumo;
import com.recred.backend.domain.models.PrediccionGasto;
import com.recred.backend.domain.models.PrediccionGastoResultado;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PrediccionGastoResponseTest {

    @Test
    void mapearPrediccionDeDominioAResponseHttp() {
        UUID alumnoId = UUID.randomUUID();
        PrediccionGasto prediccionGasto = new PrediccionGasto(
                alumnoId,
                PeriodoPresupuesto.MENSUAL,
                LocalDate.of(2026, 5, 21),
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31),
                new BigDecimal("50.00"),
                new BigDecimal("120.00"),
                new BigDecimal("10.00"),
                new BigDecimal("150.00"),
                new BigDecimal("80.00"),
                new BigDecimal("0.8000"),
                60,
                7,
                List.of(new CategoriaConsumo("Bebidas", new BigDecimal("40.00")))
        );

        PrediccionGastoResultado resultado = new PrediccionGastoResultado(
                prediccionGasto,
                Optional.of(new AnalisisPrediccionGasto(
                        "Gasto estable.",
                        List.of("Sin alertas."),
                        List.of("Mantener habitos."),
                        "gemini-test"
                ))
        );

        PrediccionGastoResponse response = PrediccionGastoResponse.fromDomain(resultado);

        assertEquals(alumnoId, response.alumnoId());
        assertEquals(PeriodoPresupuesto.MENSUAL, response.periodo());
        assertEquals(new BigDecimal("120.00"), response.gastoPredicho());
        assertEquals(1, response.categoriasMasConsumidas().size());
        assertEquals("Bebidas", response.categoriasMasConsumidas().get(0).descripcion());
        assertEquals(
                new BigDecimal("40.00"),
                response.categoriasMasConsumidas().get(0).montoTotal()
        );
        assertEquals("Gasto estable.", response.analisisIa().resumen());
        assertEquals("gemini-test", response.analisisIa().modelo());
    }
}
