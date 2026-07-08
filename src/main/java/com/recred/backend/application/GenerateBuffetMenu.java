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
    public GenerateBuffetMenuResponse execute(GenerateBuffetMenuRequest menuRequest) {
        Alumno student = findStudent(menuRequest.alumnoId());
        List<Producto> activeProducts = findActiveProductsWithStock(menuRequest.buffetId());
        LocalDateTime evaluationMoment = localBusinessMoment(menuRequest.momentoConsulta());
        BudgetAvailability budget = budgetAvailabilityFor(
                menuRequest.alumnoId(),
                menuRequest.momentoConsulta());

        return new GenerateBuffetMenuResponse(menuItemsFor(
                student,
                activeProducts,
                menuRequest.buffetId(),
                evaluationMoment,
                budget));
    }

    private Alumno findStudent(UUID alumnoId) {
        return alumnoRepository.findById(alumnoId)
                .orElseThrow(() -> new RuntimeException("Alumno no encontrado"));
    }

    private List<Producto> findActiveProductsWithStock(UUID buffetId) {
        return productRepository.findAllActiveWithStock(buffetId);
    }

    private LocalDateTime localBusinessMoment(LocalDateTime momentoConsulta) {
        return ZonedDateTime.of(momentoConsulta, ZoneId.systemDefault())
                .withZoneSameInstant(ZONA_LOCAL)
                .toLocalDateTime();
    }

    private BudgetAvailability budgetAvailabilityFor(
            UUID alumnoId,
            LocalDateTime momentoConsulta) {
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
        BigDecimal generalBudgetAvailable = presupuesto.getMontoLimiteGeneral()
                .subtract(gastadoHistorico);
        return new BudgetAvailability(
                generalBudgetAvailable,
                categoryBudgetAvailability(alumnoId, presupuesto, rango)
        );
    }

    private List<MenuProductResponse> menuItemsFor(
            Alumno student,
            List<Producto> activeProducts,
            UUID buffetId,
            LocalDateTime evaluationMoment,
            BudgetAvailability budget
    ) {
        return activeProducts.stream()
                .map(product -> menuItemFor(
                        student,
                        product,
                        buffetId,
                        evaluationMoment,
                        budget
                ))
                .toList();
    }

    private Map<UUID, BigDecimal> categoryBudgetAvailability(
            UUID alumnoId,
            PresupuestoAlumno presupuesto,
            RangoFechas rango
    ) {
        if (presupuesto.getReglasCategoria() == null) {
            return Map.of();
        }

        Map<UUID, BigDecimal> availableByCategory = new HashMap<>();
        for (ReglaPresupuestoCategoria regla : presupuesto.getReglasCategoria()) {
            UUID categoryId = regla.getCategoria().getId();
            BigDecimal spentByCategory = purchaseRepository.sumarGastosPorCategoriaEnRango(
                    alumnoId,
                    categoryId,
                    rango.inicio(),
                    rango.fin()
            );
            availableByCategory.put(
                    categoryId,
                    regla.getMontoLimiteCalculado().subtract(spentByCategory));
        }
        return availableByCategory;
    }

    private MenuProductResponse menuItemFor(
            Alumno student,
            Producto product,
            UUID buffetId,
            LocalDateTime evaluationMoment,
            BudgetAvailability budget
    ) {
        applyDailyCapacityFor(buffetId, product, evaluationMoment);
        return blockingReasonFor(student, product, evaluationMoment, budget)
                .map(reason -> MenuProductResponse.bloqueado(product, reason))
                .orElseGet(() -> MenuProductResponse.permitido(product));
    }

    private Optional<String> blockingReasonFor(
            Alumno student,
            Producto product,
            LocalDateTime evaluationMoment,
            BudgetAvailability budget
    ) {
        if (student.hasBlockedProduct(product)) {
            return Optional.of("Bloqueado por el tutor");
        }
        if (product.isDailyCapacityExhausted()) {
            return Optional.of("Cupo diario agotado");
        }
        if (!product.hasAvailableStock()) {
            return Optional.of("Sin disponibilidad");
        }
        Optional<String> healthConflict = student.healthConflictReasonFor(product)
                .map(conflict -> "Contiene: " + conflict);
        if (healthConflict.isPresent()) {
            return healthConflict;
        }
        if (student.hasTimeRestrictionFor(product, evaluationMoment)) {
            return Optional.of("No permitido en este horario");
        }
        return budget.blockingReasonFor(product);
    }

    private void applyDailyCapacityFor(
            UUID buffetId,
            Producto product,
            LocalDateTime evaluationMoment
    ) {
        if (!product.hasDailyCapacityLimit()) {
            return;
        }

        long reserved = purchaseRepository.sumScheduledProductQuantity(
                buffetId,
                product.getId(),
                evaluationMoment.toLocalDate(),
                ACTIVE_ADVANCE_STATUSES);
        product.applyDailyCapacity(reserved);
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
