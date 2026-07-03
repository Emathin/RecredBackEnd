package com.recred.backend.application;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.PresupuestoAlumno;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.RangoFechas;
import com.recred.backend.domain.models.ReglaPresupuestoCategoria;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.PresupuestoRepository;
import com.recred.backend.domain.repositories.ProductRepository;
import com.recred.backend.domain.repositories.PurchaseRepository;
import com.recred.backend.domain.services.CalculadorPeriodoPresupuesto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

public class GenerateBuffetMenu {

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

    public GenerateBuffetMenu(AlumnoRepository alumnoRepository,
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
    public GenerateBuffetMenuResponse execute(GenerateBuffetMenuRequest request) {
        UUID buffetId = requireBuffetId(request);
        Alumno alumno = findAlumno(request.alumnoId());
        List<Producto> catalogo = productRepository.findAllActiveWithStock(buffetId);
        LocalDateTime momentoLocal = localMoment(request.momentoConsulta());
        BudgetAvailability budget = budgetAvailability(request.alumnoId(),
                request.momentoConsulta());
        return new GenerateBuffetMenuResponse(menuItems(
                alumno, catalogo, buffetId, momentoLocal, budget));
    }

    private UUID requireBuffetId(GenerateBuffetMenuRequest request) {
        if (request.buffetId() == null) {
            throw new IllegalArgumentException("buffetId is required");
        }
        return request.buffetId();
    }

    private Alumno findAlumno(UUID alumnoId) {
        return alumnoRepository.findById(alumnoId)
                .orElseThrow(() -> new RuntimeException("Alumno no encontrado"));
    }

    private LocalDateTime localMoment(LocalDateTime momentoConsulta) {
        return ZonedDateTime.of(momentoConsulta, ZoneId.systemDefault())
                .withZoneSameInstant(ZONA_LOCAL)
                .toLocalDateTime();
    }

    private BudgetAvailability budgetAvailability(UUID alumnoId, LocalDateTime momentoConsulta) {
        Optional<PresupuestoAlumno> presupuestoOpt =
                presupuestoRepository.findActivoByAlumnoId(alumnoId);
        if (presupuestoOpt.isEmpty()
                || !presupuestoOpt.get().esVigente(momentoConsulta.toLocalDate())) {
            return BudgetAvailability.unlimited();
        }

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
        BigDecimal general = presupuesto.getMontoLimiteGeneral().subtract(gastadoHistorico);
        return new BudgetAvailability(
                general,
                presupuestoCategoriaDisponible(alumnoId, presupuesto, rango)
        );
    }

    private List<MenuProductResponse> menuItems(
            Alumno alumno,
            List<Producto> catalogo,
            UUID buffetId,
            LocalDateTime momentoLocal,
            BudgetAvailability budget
    ) {
        return catalogo.stream()
                .map(producto -> menuItem(
                        alumno,
                        producto,
                        buffetId,
                        momentoLocal,
                        budget
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

    private MenuProductResponse menuItem(
            Alumno alumno,
            Producto producto,
            UUID buffetId,
            LocalDateTime momentoLocal,
            BudgetAvailability budget
    ) {
        applyDailyCapacity(buffetId, producto, momentoLocal);
        return blockingReason(alumno, producto, momentoLocal, budget)
                .map(reason -> MenuProductResponse.bloqueado(producto, reason))
                .orElseGet(() -> MenuProductResponse.permitido(producto));
    }

    private Optional<String> blockingReason(
            Alumno alumno,
            Producto producto,
            LocalDateTime momentoLocal,
            BudgetAvailability budget
    ) {
        if (alumno.hasBlockedProduct(producto)) {
            return Optional.of("Bloqueado por el tutor");
        }
        if (producto.isDailyCapacityExhausted()) {
            return Optional.of("Cupo diario agotado");
        }
        if (!producto.hasAvailableStock()) {
            return Optional.of("Sin disponibilidad");
        }
        Optional<String> healthConflict = alumno.healthConflictReasonFor(producto)
                .map(conflict -> "Contiene: " + conflict);
        if (healthConflict.isPresent()) {
            return healthConflict;
        }
        if (alumno.hasTimeRestrictionFor(producto, momentoLocal)) {
            return Optional.of("No permitido en este horario");
        }
        return budget.blockingReasonFor(producto);
    }

    private void applyDailyCapacity(
            UUID buffetId,
            Producto producto,
            LocalDateTime momentoLocal
    ) {
        if (!producto.hasDailyCapacityLimit()) {
            return;
        }

        long reserved = purchaseRepository.sumScheduledProductQuantity(
                buffetId,
                producto.getId(),
                momentoLocal.toLocalDate(),
                ACTIVE_ADVANCE_STATUSES);
        producto.applyDailyCapacity(reserved);
    }

    private record BudgetAvailability(
            BigDecimal general,
            Map<UUID, BigDecimal> categories
    ) {

        static BudgetAvailability unlimited() {
            return new BudgetAvailability(null, Map.of());
        }

        Optional<String> blockingReasonFor(Producto producto) {
            if (producto.exceedsPrice(general)) {
                return Optional.of("Supera el limite de gasto");
            }
            if (producto.exceedsCategoryBudget(categories)) {
                return Optional.of("Supera limite de su categoria");
            }
            return Optional.empty();
        }
    }
}
