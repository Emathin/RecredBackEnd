package com.recred.backend.infrastructure.ia.web.controllers;

import com.recred.backend.control_parental.domain.enums.PeriodoPresupuesto;
import com.recred.backend.application.usecases.PredecirGastoAlumno;
import com.recred.backend.domain.models.AnalisisPrediccionGasto;
import com.recred.backend.domain.models.PrediccionGasto;
import com.recred.backend.domain.models.PrediccionGastoResultado;
import com.recred.backend.infrastructure.ia.web.dtos.PrediccionGastoResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PrediccionGastoControllerTest {

    @Test
    void delegarPrediccionConPeriodoSolicitado() {
        PredecirGastoAlumno predecirGastoAlumno = mock(PredecirGastoAlumno.class);
        PrediccionGastoController controller = new PrediccionGastoController(predecirGastoAlumno);
        UUID alumnoId = UUID.randomUUID();
        PrediccionGastoResultado resultado = resultado(alumnoId, PeriodoPresupuesto.MENSUAL);
        when(predecirGastoAlumno.ejecutar(alumnoId, PeriodoPresupuesto.MENSUAL)).thenReturn(resultado);

        PrediccionGastoResponse obtenido = controller.predecir(
                alumnoId,
                PeriodoPresupuesto.MENSUAL
        );

        assertEquals(alumnoId, obtenido.alumnoId());
        assertEquals(PeriodoPresupuesto.MENSUAL, obtenido.periodo());
        assertEquals("Resumen IA.", obtenido.analisisIa().resumen());
        verify(predecirGastoAlumno).ejecutar(alumnoId, PeriodoPresupuesto.MENSUAL);
    }

    @Test
    void delegarPrediccionSinPeriodoSolicitado() {
        PredecirGastoAlumno predecirGastoAlumno = mock(PredecirGastoAlumno.class);
        PrediccionGastoController controller = new PrediccionGastoController(predecirGastoAlumno);
        UUID alumnoId = UUID.randomUUID();
        PrediccionGastoResultado resultado = resultado(alumnoId, PeriodoPresupuesto.QUINCENAL);
        when(predecirGastoAlumno.ejecutar(alumnoId, null)).thenReturn(resultado);

        PrediccionGastoResponse obtenido = controller.predecir(alumnoId, null);

        assertEquals(PeriodoPresupuesto.QUINCENAL, obtenido.periodo());
        verify(predecirGastoAlumno).ejecutar(alumnoId, null);
    }

    private PrediccionGastoResultado resultado(UUID alumnoId, PeriodoPresupuesto periodo) {
        return new PrediccionGastoResultado(
                prediccion(alumnoId, periodo),
                Optional.of(new AnalisisPrediccionGasto(
                        "Resumen IA.",
                        List.of(),
                        List.of(),
                        "gemini-test"
                ))
        );
    }

    private PrediccionGasto prediccion(UUID alumnoId, PeriodoPresupuesto periodo) {
        return new PrediccionGasto(
                alumnoId,
                periodo,
                LocalDate.of(2026, 5, 21),
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                null,
                null,
                BigDecimal.ONE,
                0,
                0,
                List.of()
        );
    }
}
