package com.recred.backend.unit.domain.services;

import com.recred.backend.domain.exceptions.PresupuestoExcedidoException;
import com.recred.backend.domain.models.CategoriaProducto;
import com.recred.backend.domain.models.PeriodoPresupuesto;
import com.recred.backend.domain.models.PresupuestoAlumno;
import com.recred.backend.domain.models.RangoFechas;
import com.recred.backend.domain.models.ReglaPresupuestoCategoria;
import com.recred.backend.domain.repositories.PresupuestoRepository;
import com.recred.backend.domain.repositories.PurchaseRepository;
import com.recred.backend.domain.services.BudgetPeriodCalculator;
import com.recred.backend.domain.services.BudgetValidatorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BudgetValidatorServiceTest {

    @Mock
    private PresupuestoRepository presupuestoRepository;

    @Mock
    private PurchaseRepository compraRepository;

    @Mock
    private BudgetPeriodCalculator budgetPeriodCalculator;

    @InjectMocks
    private BudgetValidatorService validador;

    private UUID alumnoId;
    private LocalDateTime momentoCompra;
    private RangoFechas rangoSimulado;

    @BeforeEach
    void setUp() {
        alumnoId = UUID.randomUUID();
        momentoCompra = LocalDateTime.now();
        rangoSimulado = new RangoFechas(momentoCompra.minusHours(1), momentoCompra.plusHours(1));
    }

    @Test
    void validateBudget_SinPresupuestoActivo_NoHaceNada() {
        when(presupuestoRepository.findActivoByAlumnoId(alumnoId)).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> validador.validateBudget(
                alumnoId, BigDecimal.valueOf(1000), Collections.emptyMap(), momentoCompra
        ));

        verifyNoInteractions(budgetPeriodCalculator, compraRepository);
    }

    @Test
    void validateBudget_PresupuestoNoVigenteAun_NoHaceNada() {
        PresupuestoAlumno presupuesto = new PresupuestoAlumno();

        presupuesto.setFechaInicio(momentoCompra.toLocalDate().plusDays(1));

        when(presupuestoRepository.findActivoByAlumnoId(alumnoId)).thenReturn(Optional.of(presupuesto));

        assertDoesNotThrow(() -> validador.validateBudget(
                alumnoId, BigDecimal.valueOf(1000), Collections.emptyMap(), momentoCompra
        ));

        verifyNoInteractions(budgetPeriodCalculator, compraRepository);
    }

    @Test
    void validateBudget_SuperaLimiteGeneral_LanzaExcepcion() {
        PresupuestoAlumno presupuesto = crearPresupuestoBase(BigDecimal.valueOf(2000));
        when(presupuestoRepository.findActivoByAlumnoId(alumnoId)).thenReturn(Optional.of(presupuesto));
        when(budgetPeriodCalculator.calculateRange(any(), any())).thenReturn(rangoSimulado);

        when(compraRepository.sumarGastosPorAlumnoEnRango(eq(alumnoId), any(), any()))
                .thenReturn(BigDecimal.valueOf(1500));

        PresupuestoExcedidoException ex = assertThrows(PresupuestoExcedidoException.class, () -> {
            validador.validateBudget(alumnoId, BigDecimal.valueOf(600), Collections.emptyMap(), momentoCompra);
        });

        assertTrue(ex.getMessage().contains("Supera el presupuesto diario"));
    }

    @Test
    void validateBudget_SuperaLimiteDeCategoria_LanzaExcepcion() {

        PresupuestoAlumno presupuesto = crearPresupuestoBase(BigDecimal.valueOf(5000));

        UUID golosinasId = UUID.randomUUID();
        CategoriaProducto golosinas = new CategoriaProducto();
        golosinas.setId(golosinasId);
        golosinas.setDescripcion("Golosinas");

        ReglaPresupuestoCategoria reglaGolosinas = new ReglaPresupuestoCategoria(
                UUID.randomUUID(), golosinas, BigDecimal.valueOf(20), BigDecimal.valueOf(1000), true
        );
        presupuesto.setReglasCategoria(List.of(reglaGolosinas));

        when(presupuestoRepository.findActivoByAlumnoId(alumnoId)).thenReturn(Optional.of(presupuesto));
        when(budgetPeriodCalculator.calculateRange(any(), any())).thenReturn(rangoSimulado);

        when(compraRepository.sumarGastosPorAlumnoEnRango(eq(alumnoId), any(), any()))
                .thenReturn(BigDecimal.ZERO);

        when(compraRepository.sumarGastosPorCategoriaEnRango(eq(alumnoId), eq(golosinasId), any(), any()))
                .thenReturn(BigDecimal.valueOf(800));

        Map<UUID, BigDecimal> gastosPorCategoria = Map.of(golosinasId, BigDecimal.valueOf(300));

        PresupuestoExcedidoException ex = assertThrows(PresupuestoExcedidoException.class, () -> {
            validador.validateBudget(alumnoId, BigDecimal.valueOf(500), gastosPorCategoria, momentoCompra);
        });

        assertTrue(ex.getMessage().contains("Supera el límite diario de Golosinas"));
    }

    @Test
    void validateBudget_TodoDentroDeLosLimites_NoLanzaExcepcion() {
        PresupuestoAlumno presupuesto = crearPresupuestoBase(BigDecimal.valueOf(2000));
        when(presupuestoRepository.findActivoByAlumnoId(alumnoId)).thenReturn(Optional.of(presupuesto));
        when(budgetPeriodCalculator.calculateRange(any(), any())).thenReturn(rangoSimulado);

        when(compraRepository.sumarGastosPorAlumnoEnRango(eq(alumnoId), any(), any()))
                .thenReturn(BigDecimal.valueOf(1000));

        assertDoesNotThrow(() -> {
            validador.validateBudget(alumnoId, BigDecimal.valueOf(500), Collections.emptyMap(), momentoCompra);
        });
    }

    @Test
    void validateBudget_ConReglaSinGastoEnCarrito_NoConsultaGastoDeCategoria() {
        UUID categoriaId = UUID.randomUUID();
        PresupuestoAlumno presupuesto = crearPresupuestoBase(BigDecimal.valueOf(2000));
        presupuesto.setReglasCategoria(List.of(reglaCategoria(categoriaId, "Bebidas")));
        when(presupuestoRepository.findActivoByAlumnoId(alumnoId)).thenReturn(Optional.of(presupuesto));
        when(budgetPeriodCalculator.calculateRange(any(), any())).thenReturn(rangoSimulado);
        when(compraRepository.sumarGastosPorAlumnoEnRango(eq(alumnoId), any(), any()))
                .thenReturn(BigDecimal.ZERO);

        assertDoesNotThrow(() -> validador.validateBudget(
                alumnoId,
                BigDecimal.valueOf(100),
                Collections.emptyMap(),
                momentoCompra
        ));

        verify(compraRepository, never()).sumarGastosPorCategoriaEnRango(
                eq(alumnoId), eq(categoriaId), any(), any()
        );
    }

    @Test
    void getBudgetBlockReason_ShouldFormatAllBudgetPeriods() {
        PresupuestoAlumno nullPeriod = crearPresupuestoBase(BigDecimal.valueOf(1000));
        nullPeriod.setPeriodo(null);
        PresupuestoAlumno weekly = crearPresupuestoBase(BigDecimal.valueOf(1000));
        weekly.setPeriodo(PeriodoPresupuesto.SEMANAL);
        PresupuestoAlumno biweekly = crearPresupuestoBase(BigDecimal.valueOf(1000));
        biweekly.setPeriodo(PeriodoPresupuesto.QUINCENAL);
        PresupuestoAlumno monthly = crearPresupuestoBase(BigDecimal.valueOf(1000));
        monthly.setPeriodo(PeriodoPresupuesto.MENSUAL);
        when(presupuestoRepository.findActivoByAlumnoId(alumnoId))
                .thenReturn(
                        Optional.of(nullPeriod),
                        Optional.of(weekly),
                        Optional.of(biweekly),
                        Optional.of(monthly)
                );
        when(budgetPeriodCalculator.calculateRange(any(), any())).thenReturn(rangoSimulado);
        when(compraRepository.sumarGastosPorAlumnoEnRango(eq(alumnoId), any(), any()))
                .thenReturn(BigDecimal.valueOf(900));

        assertEquals("Supera el presupuesto diario: disponible $100.00",
                motivoBloqueo(BigDecimal.valueOf(200)).orElseThrow());
        assertEquals("Supera el presupuesto semanal: disponible $100.00",
                motivoBloqueo(BigDecimal.valueOf(200)).orElseThrow());
        assertEquals("Supera el presupuesto quincenal: disponible $100.00",
                motivoBloqueo(BigDecimal.valueOf(200)).orElseThrow());
        assertEquals("Supera el presupuesto mensual: disponible $100.00",
                motivoBloqueo(BigDecimal.valueOf(200)).orElseThrow());
    }

    private Optional<String> motivoBloqueo(BigDecimal monto) {
        return validador.getBudgetBlockReason(
                alumnoId,
                monto,
                Collections.emptyMap(),
                momentoCompra
        );
    }

    private ReglaPresupuestoCategoria reglaCategoria(UUID categoriaId, String descripcion) {
        CategoriaProducto categoria = new CategoriaProducto();
        categoria.setId(categoriaId);
        categoria.setDescripcion(descripcion);
        return new ReglaPresupuestoCategoria(
                UUID.randomUUID(),
                categoria,
                BigDecimal.valueOf(20),
                BigDecimal.valueOf(1000),
                true
        );
    }

    private PresupuestoAlumno crearPresupuestoBase(BigDecimal limiteGeneral) {
        PresupuestoAlumno p = new PresupuestoAlumno();
        p.setId(UUID.randomUUID());
        p.setActivo(true);
        p.setFechaInicio(LocalDate.now().minusDays(1));
        p.setPeriodo(PeriodoPresupuesto.DIARIO);
        p.setMontoLimiteGeneral(limiteGeneral);
        return p;
    }
}
