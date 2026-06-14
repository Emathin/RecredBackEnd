package com.recred.backend.domain.services;

import com.recred.backend.domain.exceptions.PresupuestoExcedidoException;
import com.recred.backend.domain.models.CategoriaProducto;
import com.recred.backend.domain.models.PeriodoPresupuesto;
import com.recred.backend.domain.models.PresupuestoAlumno;
import com.recred.backend.domain.models.RangoFechas;
import com.recred.backend.domain.models.ReglaPresupuestoCategoria;
import com.recred.backend.domain.repositories.PresupuestoRepository;
import com.recred.backend.domain.repositories.PurchaseRepository;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValidadorPresupuestoServiceTest {

    @Mock
    private PresupuestoRepository presupuestoRepository;

    @Mock
    private PurchaseRepository compraRepository;

    @Mock
    private CalculadorPeriodoPresupuesto calculadorPeriodo;

    @InjectMocks
    private ValidadorPresupuestoService validador;

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
    void validarPresupuesto_SinPresupuestoActivo_NoHaceNada() {
        when(presupuestoRepository.findActivoByAlumnoId(alumnoId)).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> validador.validarPresupuesto(
                alumnoId, BigDecimal.valueOf(1000), Collections.emptyMap(), momentoCompra
        ));

        verifyNoInteractions(calculadorPeriodo, compraRepository);
    }

    @Test
    void validarPresupuesto_PresupuestoNoVigenteAun_NoHaceNada() {
        PresupuestoAlumno presupuesto = new PresupuestoAlumno();

        presupuesto.setFechaInicio(momentoCompra.toLocalDate().plusDays(1));

        when(presupuestoRepository.findActivoByAlumnoId(alumnoId)).thenReturn(Optional.of(presupuesto));

        assertDoesNotThrow(() -> validador.validarPresupuesto(
                alumnoId, BigDecimal.valueOf(1000), Collections.emptyMap(), momentoCompra
        ));

        verifyNoInteractions(calculadorPeriodo, compraRepository);
    }

    @Test
    void validarPresupuesto_SuperaLimiteGeneral_LanzaExcepcion() {
        PresupuestoAlumno presupuesto = crearPresupuestoBase(BigDecimal.valueOf(2000));
        when(presupuestoRepository.findActivoByAlumnoId(alumnoId)).thenReturn(Optional.of(presupuesto));
        when(calculadorPeriodo.calcularRango(any(), any())).thenReturn(rangoSimulado);

        when(compraRepository.sumarGastosPorAlumnoEnRango(eq(alumnoId), any(), any()))
                .thenReturn(BigDecimal.valueOf(1500));

        PresupuestoExcedidoException ex = assertThrows(PresupuestoExcedidoException.class, () -> {
            validador.validarPresupuesto(alumnoId, BigDecimal.valueOf(600), Collections.emptyMap(), momentoCompra);
        });

        assertTrue(ex.getMessage().contains("Supera el presupuesto diario"));
    }

    @Test
    void validarPresupuesto_SuperaLimiteDeCategoria_LanzaExcepcion() {

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
        when(calculadorPeriodo.calcularRango(any(), any())).thenReturn(rangoSimulado);

        when(compraRepository.sumarGastosPorAlumnoEnRango(eq(alumnoId), any(), any()))
                .thenReturn(BigDecimal.ZERO);

        when(compraRepository.sumarGastosPorCategoriaEnRango(eq(alumnoId), eq(golosinasId), any(), any()))
                .thenReturn(BigDecimal.valueOf(800));

        Map<UUID, BigDecimal> gastosPorCategoria = Map.of(golosinasId, BigDecimal.valueOf(300));

        PresupuestoExcedidoException ex = assertThrows(PresupuestoExcedidoException.class, () -> {
            validador.validarPresupuesto(alumnoId, BigDecimal.valueOf(500), gastosPorCategoria, momentoCompra);
        });

        assertTrue(ex.getMessage().contains("Supera el límite diario de Golosinas"));
    }

    @Test
    void validarPresupuesto_TodoDentroDeLosLimites_NoLanzaExcepcion() {
        PresupuestoAlumno presupuesto = crearPresupuestoBase(BigDecimal.valueOf(2000));
        when(presupuestoRepository.findActivoByAlumnoId(alumnoId)).thenReturn(Optional.of(presupuesto));
        when(calculadorPeriodo.calcularRango(any(), any())).thenReturn(rangoSimulado);

        when(compraRepository.sumarGastosPorAlumnoEnRango(eq(alumnoId), any(), any()))
                .thenReturn(BigDecimal.valueOf(1000));

        assertDoesNotThrow(() -> {
            validador.validarPresupuesto(alumnoId, BigDecimal.valueOf(500), Collections.emptyMap(), momentoCompra);
        });
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