package com.recred.backend.application.usecases;

import com.recred.backend.control_parental.domain.enums.PeriodoPresupuesto;
import com.recred.backend.domain.services.AnalizadorPrediccionGasto;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.CompraGastoRepository;
import com.recred.backend.domain.repositories.PreferenciaGastoRepository;
import com.recred.backend.domain.repositories.EventoIARepository;
import com.recred.backend.domain.exceptions.AlumnoNoEncontradoException;
import com.recred.backend.domain.models.AnalisisPrediccionGasto;
import com.recred.backend.domain.models.CategoriaConsumo;
import com.recred.backend.domain.models.GastoCompra;
import com.recred.backend.domain.models.PrediccionGasto;
import com.recred.backend.domain.models.PrediccionGastoResultado;
import com.recred.backend.domain.models.PreferenciaPeriodoGasto;
import com.recred.backend.domain.services.PrediccionGastoCalculator;
import com.recred.backend.domain.services.RangoPrediccionGastoService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PredecirGastoAlumnoTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-05-21T12:00:00Z"),
            ZoneId.of("UTC")
    );

    @Mock
    private AlumnoRepository alumnoRepository;

    @Mock
    private PreferenciaGastoRepository preferenciaGastoRepository;

    @Mock
    private CompraGastoRepository compraGastoRepository;

    @Mock
    private AnalizadorPrediccionGasto analizadorPrediccionGasto;

    @Mock
    private EventoIARepository eventoIARepository;

    private PredecirGastoAlumno predecirGastoAlumno;

    @BeforeEach
    void setUp() {
        predecirGastoAlumno = new PredecirGastoAlumno(
                alumnoRepository,
                preferenciaGastoRepository,
                compraGastoRepository,
                analizadorPrediccionGasto,
                eventoIARepository,
                new RangoPrediccionGastoService(),
                new PrediccionGastoCalculator(),
                CLOCK
        );
    }

    @Test
    void usarPreferenciaActivaCuandoNoSeSolicitaPeriodo() {
        UUID alumnoId = UUID.randomUUID();
        when(alumnoRepository.existsById(alumnoId)).thenReturn(true);
        when(preferenciaGastoRepository.buscarPreferenciasActivas(
                alumnoId,
                LocalDate.of(2026, 5, 21)
        )).thenReturn(List.of(new PreferenciaPeriodoGasto(
                PeriodoPresupuesto.QUINCENAL,
                new BigDecimal("200.00")
        )));
        when(compraGastoRepository.buscarGastos(any(), anyCollection(), any(), any()))
                .thenReturn(
                        List.of(gasto("2026-05-21T10:00:00", "50.00")),
                        List.of(gasto("2026-04-10T10:00:00", "100.00"))
                );
        when(compraGastoRepository.buscarCategoriasMasConsumidas(
                any(),
                anyCollection(),
                any(),
                any()
        ))
                .thenReturn(List.of(
                        categoria("Bebidas", "20.00"),
                        categoria("Snacks", "15.00"),
                        categoria("Frutas", "10.00"),
                        categoria("Panificados", "8.00"),
                        categoria("Golosinas", "5.00"),
                        categoria("Otros", "1.00")
                ));
        when(analizadorPrediccionGasto.analizar(any())).thenReturn(Optional.of(analisis()));

        PrediccionGastoResultado response = predecirGastoAlumno.ejecutar(alumnoId, null);

        assertEquals(PeriodoPresupuesto.QUINCENAL, response.prediccionGasto().periodo());
        assertEquals(new BigDecimal("200.00"), response.prediccionGasto().montoLimite());
        assertEquals(5, response.prediccionGasto().categoriasMasConsumidas().size());
        assertEquals("Reducir snacks.", response.analisisIa().orElseThrow().recomendaciones()
                .get(0));
        verify(preferenciaGastoRepository).buscarPreferenciasActivas(
                alumnoId,
                LocalDate.of(2026, 5, 21)
        );
        ArgumentCaptor<PrediccionGastoResultado> captor = ArgumentCaptor.forClass(
                PrediccionGastoResultado.class);
        verify(eventoIARepository).registrarPrediccion(captor.capture());
        assertEquals(PeriodoPresupuesto.QUINCENAL, captor.getValue().prediccionGasto().periodo());
    }

    @Test
    void periodoSolicitadoSobrescribePreferencia() {
        UUID alumnoId = UUID.randomUUID();
        when(alumnoRepository.existsById(alumnoId)).thenReturn(true);
        when(preferenciaGastoRepository.buscarPreferenciasActivas(any(), any()))
                .thenReturn(List.of(new PreferenciaPeriodoGasto(
                        PeriodoPresupuesto.QUINCENAL,
                        new BigDecimal("200.00")
                )));
        when(compraGastoRepository.buscarGastos(any(), anyCollection(), any(), any()))
                .thenReturn(List.of(), List.of());
        when(compraGastoRepository.buscarCategoriasMasConsumidas(
                any(),
                anyCollection(),
                any(),
                any()
        ))
                .thenReturn(List.of());
        when(analizadorPrediccionGasto.analizar(any())).thenReturn(Optional.empty());

        PrediccionGastoResultado response = predecirGastoAlumno.ejecutar(
                alumnoId,
                PeriodoPresupuesto.MENSUAL
        );

        assertEquals(PeriodoPresupuesto.MENSUAL, response.prediccionGasto().periodo());
        verify(eventoIARepository).registrarPrediccion(any(PrediccionGastoResultado.class));
    }

    @Test
    void usarPeriodoMensualCuandoNoHayPreferenciaNiPeriodoSolicitado() {
        UUID alumnoId = UUID.randomUUID();
        when(alumnoRepository.existsById(alumnoId)).thenReturn(true);
        when(preferenciaGastoRepository.buscarPreferenciasActivas(any(), any()))
                .thenReturn(List.of());
        when(compraGastoRepository.buscarGastos(any(), anyCollection(), any(), any()))
                .thenReturn(List.of(), List.of());
        when(compraGastoRepository.buscarCategoriasMasConsumidas(
                any(),
                anyCollection(),
                any(),
                any()
        ))
                .thenReturn(List.of());
        when(analizadorPrediccionGasto.analizar(any())).thenReturn(Optional.empty());

        PrediccionGastoResultado response = predecirGastoAlumno.ejecutar(alumnoId, null);

        assertEquals(PeriodoPresupuesto.MENSUAL, response.prediccionGasto().periodo());
    }

    @Test
    void lanzarExcepcionCuandoAlumnoNoExiste() {
        UUID alumnoId = UUID.randomUUID();
        when(alumnoRepository.existsById(alumnoId)).thenReturn(false);

        assertThrows(AlumnoNoEncontradoException.class, () -> predecirGastoAlumno.ejecutar(
                alumnoId,
                PeriodoPresupuesto.MENSUAL
        ));
        verify(preferenciaGastoRepository, never()).buscarPreferenciasActivas(any(), any());
        verify(compraGastoRepository, never()).buscarGastos(
                any(),
                anyCollection(),
                any(),
                any()
        );
        verify(analizadorPrediccionGasto, never()).analizar(any());
        verify(eventoIARepository, never()).registrarPrediccion(any());
    }

    @Test
    void consultarGastosConRangosDelPeriodoActualYHistorico() {
        UUID alumnoId = UUID.randomUUID();
        when(alumnoRepository.existsById(alumnoId)).thenReturn(true);
        when(preferenciaGastoRepository.buscarPreferenciasActivas(any(), any()))
                .thenReturn(List.of());
        when(compraGastoRepository.buscarGastos(any(), anyCollection(), any(), any()))
                .thenReturn(List.of(), List.of());
        when(compraGastoRepository.buscarCategoriasMasConsumidas(
                any(),
                anyCollection(),
                any(),
                any()
        ))
                .thenReturn(List.of());
        when(analizadorPrediccionGasto.analizar(any())).thenReturn(Optional.empty());

        predecirGastoAlumno.ejecutar(alumnoId, PeriodoPresupuesto.MENSUAL);

        verify(compraGastoRepository).buscarGastos(
                eq(alumnoId),
                anyCollection(),
                eq(LocalDateTime.parse("2026-05-01T00:00:00")),
                eq(LocalDateTime.parse("2026-05-21T23:59:59.999999999"))
        );
        verify(compraGastoRepository).buscarGastos(
                eq(alumnoId),
                anyCollection(),
                eq(LocalDateTime.parse("2026-01-31T00:00:00")),
                eq(LocalDateTime.parse("2026-04-30T23:59:59.999999999"))
        );
    }

    private GastoCompra gasto(String fechaHora, String monto) {
        return new GastoCompra(LocalDateTime.parse(fechaHora), new BigDecimal(monto));
    }

    private CategoriaConsumo categoria(String descripcion, String monto) {
        return new CategoriaConsumo(descripcion, new BigDecimal(monto));
    }

    private AnalisisPrediccionGasto analisis() {
        return new AnalisisPrediccionGasto(
                "Puede superar el presupuesto.",
                List.of("Alerta de gasto."),
                List.of("Reducir snacks."),
                "gemini-test"
        );
    }
}
