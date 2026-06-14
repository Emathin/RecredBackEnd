package com.recred.backend.application;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.ClasificacionSalud;
import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.PresupuestoAlumno;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.RangoFechas;
import com.recred.backend.domain.models.ReglaPresupuestoCategoria;
import com.recred.backend.domain.models.TipoManejoInventario;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.PresupuestoRepository;
import com.recred.backend.domain.repositories.ProductRepository;
import com.recred.backend.domain.repositories.PurchaseRepository;
import com.recred.backend.domain.services.CalculadorPeriodoPresupuesto;
import com.recred.backend.infrastructure.web.MenuProductoDTO;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Transactional;

public class GenerarMenuBuffet {

    private static final ZoneId ZONA_LOCAL = ZoneId.of("America/Argentina/Buenos_Aires");
    private static final List<EstadoCompra> ACTIVE_ADVANCE_STATUSES = List.of(
            EstadoCompra.PENDIENTE,
            EstadoCompra.EN_PREPARACION,
            EstadoCompra.LISTO
    );

    private final AlumnoRepository alumnoRepository;
    private final ProductRepository productRepository;
    private final PresupuestoRepository presupuestoRepository;
    private final PurchaseRepository purchaseRepository;
    private final CalculadorPeriodoPresupuesto calculadorPeriodo;

    public GenerarMenuBuffet(AlumnoRepository alumnoRepository,
                             ProductRepository productRepository,
                             PresupuestoRepository presupuestoRepository,
                             PurchaseRepository purchaseRepository,
                             CalculadorPeriodoPresupuesto calculadorPeriodo) {
        this.alumnoRepository = alumnoRepository;
        this.productRepository = productRepository;
        this.presupuestoRepository = presupuestoRepository;
        this.purchaseRepository = purchaseRepository;
        this.calculadorPeriodo = calculadorPeriodo;
    }

    @Transactional(readOnly = true)
    public List<MenuProductoDTO> execute(
            UUID alumnoId,
            UUID buffetId,
            LocalDateTime momentoConsulta
    ) {
        if (buffetId == null) {
            throw new IllegalArgumentException("buffetId is required");
        }

        Alumno alumno = alumnoRepository.findById(alumnoId)
                .orElseThrow(() -> new RuntimeException("Alumno no encontrado"));
        List<Producto> catalogo = productRepository.findAllActiveWithStock(buffetId);

        LocalDateTime momentoLocal = ZonedDateTime.of(momentoConsulta, ZoneId.systemDefault())
                .withZoneSameInstant(ZONA_LOCAL).toLocalDateTime();

        BigDecimal presupuestoGeneralDisponible = null;
        Map<UUID, BigDecimal> presupuestoCategoriaDisponible = new HashMap<>();

        Optional<PresupuestoAlumno> presupuestoOpt =
                presupuestoRepository.findActivoByAlumnoId(alumnoId);
        if (presupuestoOpt.isPresent()
                && presupuestoOpt.get().esVigente(momentoConsulta.toLocalDate())) {
            PresupuestoAlumno presupuesto = presupuestoOpt.get();
            RangoFechas rango = calculadorPeriodo.calcularRango(
                    presupuesto.getPeriodo(),
                    momentoConsulta
            );
            BigDecimal gastadoHistorico = purchaseRepository.sumarGastosPorAlumnoEnRango(
                    alumnoId,
                    rango.inicio(),
                    rango.fin()
            );
            presupuestoGeneralDisponible = presupuesto.getMontoLimiteGeneral()
                    .subtract(gastadoHistorico);
            presupuestoCategoriaDisponible = presupuestoCategoriaDisponible(
                    alumnoId,
                    presupuesto,
                    rango
            );
        }

        BigDecimal presupuestoFinalParaLambda = presupuestoGeneralDisponible;
        Map<UUID, BigDecimal> presupuestoCategoriaFinal = presupuestoCategoriaDisponible;

        return catalogo.stream()
                .map(producto -> menuItem(
                        alumno,
                        producto,
                        buffetId,
                        momentoLocal,
                        presupuestoFinalParaLambda,
                        presupuestoCategoriaFinal
                ))
                .toList();
    }

    private Map<UUID, BigDecimal> presupuestoCategoriaDisponible(
            UUID alumnoId,
            PresupuestoAlumno presupuesto,
            RangoFechas rango
    ) {
        if (presupuesto.getReglasCategoria() == null) {
            return Map.of();
        }

        Map<UUID, BigDecimal> disponible = new HashMap<>();
        for (ReglaPresupuestoCategoria regla : presupuesto.getReglasCategoria()) {
            UUID catId = regla.getCategoria().getId();
            BigDecimal gastadoCat = purchaseRepository.sumarGastosPorCategoriaEnRango(
                    alumnoId,
                    catId,
                    rango.inicio(),
                    rango.fin()
            );
            disponible.put(catId, regla.getMontoLimiteCalculado().subtract(gastadoCat));
        }
        return disponible;
    }

    private MenuProductoDTO menuItem(
            Alumno alumno,
            Producto producto,
            UUID buffetId,
            LocalDateTime momentoLocal,
            BigDecimal presupuestoGeneralDisponible,
            Map<UUID, BigDecimal> presupuestoCategoriaDisponible
    ) {
        applyDailyCapacity(buffetId, producto, momentoLocal);

        if (estaBloqueadoManual(alumno, producto)) {
            return MenuProductoDTO.bloqueado(producto, "Bloqueado por el tutor");
        }

        if (sinCupoDiario(producto)) {
            return MenuProductoDTO.bloqueado(producto, "Cupo diario agotado");
        }

        if (!tieneDisponibilidad(producto)) {
            return MenuProductoDTO.bloqueado(producto, "Sin disponibilidad");
        }

        Optional<String> conflictosSalud = conflictosSalud(alumno, producto);
        if (conflictosSalud.isPresent()) {
            return MenuProductoDTO.bloqueado(producto, "Contiene: " + conflictosSalud.get());
        }

        if (fueraDeHorario(alumno, producto, momentoLocal)) {
            return MenuProductoDTO.bloqueado(producto, "No permitido en este horario");
        }

        if (superaPresupuestoGeneral(producto, presupuestoGeneralDisponible)) {
            return MenuProductoDTO.bloqueado(producto, "Supera el limite de gasto");
        }

        if (superaPresupuestoCategoria(producto, presupuestoCategoriaDisponible)) {
            return MenuProductoDTO.bloqueado(producto, "Supera limite de su categoria");
        }

        return MenuProductoDTO.permitido(producto);
    }

    private boolean estaBloqueadoManual(Alumno alumno, Producto producto) {
        if (alumno.getProductosBloqueados() == null) {
            return false;
        }

        return alumno.getProductosBloqueados().stream()
                .anyMatch(bloqueado -> bloqueado.getId().equals(producto.getId()));
    }

    private boolean tieneDisponibilidad(Producto producto) {
        return producto.getStockDisponible() != null && producto.getStockDisponible() > 0;
    }

    private void applyDailyCapacity(
            UUID buffetId,
            Producto producto,
            LocalDateTime momentoLocal
    ) {
        if (!hasDailyCapacity(producto)) {
            return;
        }

        long reserved = purchaseRepository.sumScheduledProductQuantity(
                buffetId,
                producto.getId(),
                momentoLocal.toLocalDate(),
                ACTIVE_ADVANCE_STATUSES);
        producto.setCupoDisponibleDia(Math.max(0,
                producto.getCupoMaximoDiario() - (int) reserved));
    }

    private boolean hasDailyCapacity(Producto producto) {
        return producto.getTipoManejoInventario() == TipoManejoInventario.CUPO_DIARIO
                && producto.getCupoMaximoDiario() != null
                && producto.getCupoMaximoDiario() > 0;
    }

    private boolean sinCupoDiario(Producto producto) {
        return hasDailyCapacity(producto)
                && producto.getCupoDisponibleDia() != null
                && producto.getCupoDisponibleDia() <= 0;
    }

    private Optional<String> conflictosSalud(Alumno alumno, Producto producto) {
        if (alumno.getRestriccionesSalud() == null) {
            return Optional.empty();
        }

        String conflictos = alumno.getRestriccionesSalud().stream()
                .filter(restriccion -> producto.getClasificaciones().stream()
                        .anyMatch(c -> c.getId().equals(restriccion.getId())))
                .map(ClasificacionSalud::getDescripcion)
                .collect(Collectors.joining(", "));

        return conflictos.isEmpty() ? Optional.empty() : Optional.of(conflictos);
    }

    private boolean fueraDeHorario(
            Alumno alumno,
            Producto producto,
            LocalDateTime momentoLocal
    ) {
        if (alumno.getRestriccionesHorarias() == null) {
            return false;
        }

        return alumno.getRestriccionesHorarias().stream()
                .anyMatch(restriccion -> restriccion.bloquea(producto, momentoLocal));
    }

    private boolean superaPresupuestoGeneral(
            Producto producto,
            BigDecimal presupuestoGeneralDisponible
    ) {
        return presupuestoGeneralDisponible != null
                && producto.getPrecio().compareTo(presupuestoGeneralDisponible) > 0;
    }

    private boolean superaPresupuestoCategoria(
            Producto producto,
            Map<UUID, BigDecimal> presupuestoCategoriaDisponible
    ) {
        if (producto.getCategoria() == null) {
            return false;
        }

        BigDecimal disponible = presupuestoCategoriaDisponible.get(producto.getCategoria().getId());
        return disponible != null && producto.getPrecio().compareTo(disponible) > 0;
    }
}
