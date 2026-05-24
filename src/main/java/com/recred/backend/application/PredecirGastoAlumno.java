package com.recred.backend.application;

import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.PeriodoPresupuesto;
import com.recred.backend.domain.models.AnalisisPrediccionGasto;
import com.recred.backend.domain.exceptions.AlumnoNoEncontradoException;
import com.recred.backend.domain.models.CategoriaConsumo;
import com.recred.backend.domain.models.GastoCompra;
import com.recred.backend.domain.models.PrediccionGasto;
import com.recred.backend.domain.models.PrediccionGastoResultado;
import com.recred.backend.domain.models.PreferenciaPeriodoGasto;
import com.recred.backend.domain.models.RangoFecha;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.CompraGastoRepository;
import com.recred.backend.domain.repositories.EventoIARepository;
import com.recred.backend.domain.repositories.PreferenciaGastoRepository;
import com.recred.backend.domain.services.AnalizadorPrediccionGasto;
import com.recred.backend.domain.services.PrediccionGastoCalculator;
import com.recred.backend.domain.services.RangoPrediccionGastoService;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PredecirGastoAlumno {

    private static final int DIAS_HISTORICOS = 90;
    private static final int MAX_CATEGORIAS = 5;
    private static final Set<EstadoCompra> ESTADOS_GASTO_REAL = Set.of(EstadoCompra.ENTREGADO);

    private final AlumnoRepository alumnoRepository;
    private final PreferenciaGastoRepository preferenciaGastoRepository;
    private final CompraGastoRepository compraGastoRepository;
    private final AnalizadorPrediccionGasto analizadorPrediccionGasto;
    private final EventoIARepository eventoIARepository;
    private final RangoPrediccionGastoService rangoPrediccionGastoService;
    private final PrediccionGastoCalculator prediccionGastoCalculator;
    private final Clock clock;

    public PredecirGastoAlumno(
            AlumnoRepository alumnoRepository,
            PreferenciaGastoRepository preferenciaGastoRepository,
            CompraGastoRepository compraGastoRepository,
            AnalizadorPrediccionGasto analizadorPrediccionGasto,
            EventoIARepository eventoIARepository,
            RangoPrediccionGastoService rangoPrediccionGastoService,
            PrediccionGastoCalculator prediccionGastoCalculator,
            Clock clock
    ) {
        this.alumnoRepository = alumnoRepository;
        this.preferenciaGastoRepository = preferenciaGastoRepository;
        this.compraGastoRepository = compraGastoRepository;
        this.analizadorPrediccionGasto = analizadorPrediccionGasto;
        this.eventoIARepository = eventoIARepository;
        this.rangoPrediccionGastoService = rangoPrediccionGastoService;
        this.prediccionGastoCalculator = prediccionGastoCalculator;
        this.clock = clock;
    }

    @Transactional
    public PrediccionGastoResultado ejecutar(UUID alumnoId, PeriodoPresupuesto periodoSolicitado) {
        if (!alumnoRepository.existsById(alumnoId)) {
            throw new AlumnoNoEncontradoException(alumnoId);
        }

        LocalDate fechaCalculo = LocalDate.now(clock);
        Optional<PreferenciaPeriodoGasto> preferencia = obtenerPreferencia(alumnoId, fechaCalculo);
        PeriodoPresupuesto periodo = resolverPeriodo(periodoSolicitado, preferencia);
        RangoFecha rangoActual = rangoPrediccionGastoService.calcular(periodo, fechaCalculo);
        RangoFecha rangoHistorico = new RangoFecha(
                rangoActual.fechaInicio().minusDays(DIAS_HISTORICOS),
                rangoActual.fechaInicio().minusDays(1)
        );

        List<GastoCompra> gastosPeriodoActual = compraGastoRepository.buscarGastos(
                alumnoId,
                ESTADOS_GASTO_REAL,
                inicioDelDia(rangoActual.fechaInicio()),
                finDelDia(fechaCalculo)
        );
        List<GastoCompra> gastosHistoricos = compraGastoRepository.buscarGastos(
                alumnoId,
                ESTADOS_GASTO_REAL,
                inicioDelDia(rangoHistorico.fechaInicio()),
                finDelDia(rangoHistorico.fechaFin())
        );
        List<CategoriaConsumo> categorias = compraGastoRepository
                .buscarCategoriasMasConsumidas(
                        alumnoId,
                        ESTADOS_GASTO_REAL,
                        inicioDelDia(rangoActual.fechaInicio()),
                        finDelDia(fechaCalculo)
                )
                .stream()
                .limit(MAX_CATEGORIAS)
                .toList();

        PrediccionGasto prediccionGasto = prediccionGastoCalculator.calcular(
                alumnoId,
                periodo,
                fechaCalculo,
                rangoActual,
                rangoHistorico,
                gastosPeriodoActual,
                gastosHistoricos,
                preferencia,
                categorias
        );
        Optional<AnalisisPrediccionGasto> analisisIa = analizadorPrediccionGasto
                .analizar(prediccionGasto);
        PrediccionGastoResultado resultado = new PrediccionGastoResultado(
                prediccionGasto,
                analisisIa
        );
        eventoIARepository.registrarPrediccion(resultado);
        return resultado;
    }

    private Optional<PreferenciaPeriodoGasto> obtenerPreferencia(
            UUID alumnoId,
            LocalDate fechaCalculo
    ) {
        return preferenciaGastoRepository
                .buscarPreferenciasActivas(alumnoId, fechaCalculo)
                .stream()
                .findFirst();
    }

    private PeriodoPresupuesto resolverPeriodo(
            PeriodoPresupuesto periodoSolicitado,
            Optional<PreferenciaPeriodoGasto> preferencia
    ) {
        if (periodoSolicitado != null) {
            return periodoSolicitado;
        }
        return preferencia
                .map(PreferenciaPeriodoGasto::periodo)
                .orElse(PeriodoPresupuesto.MENSUAL);
    }

    private LocalDateTime inicioDelDia(LocalDate fecha) {
        return fecha.atStartOfDay();
    }

    private LocalDateTime finDelDia(LocalDate fecha) {
        return fecha.atTime(LocalTime.MAX);
    }
}
