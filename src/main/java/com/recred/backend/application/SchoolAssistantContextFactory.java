package com.recred.backend.application;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Billetera;
import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.PresupuestoAlumno;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.SchoolAssistantContext;
import com.recred.backend.domain.models.SchoolAssistantPayment;
import com.recred.backend.domain.models.SchoolAssistantPurchase;
import com.recred.backend.domain.models.SchoolAssistantRole;
import com.recred.backend.domain.models.SchoolCalendarEvent;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.repositories.PresupuestoRepository;
import com.recred.backend.domain.repositories.ProductRepository;
import com.recred.backend.domain.repositories.PurchaseRepository;
import com.recred.backend.domain.repositories.SchoolAssistantPaymentRepository;
import com.recred.backend.domain.repositories.SchoolAssistantPurchaseRepository;
import com.recred.backend.domain.repositories.SchoolCalendarRepository;
import com.recred.backend.domain.repositories.WalletRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

class SchoolAssistantContextFactory {

    private static final int RECENT_PAYMENT_LIMIT = 5;
    private static final int CHILD_PAYMENT_LIMIT = 2;
    private static final int RECENT_PURCHASE_LIMIT = 10;
    private static final int UPCOMING_EVENT_DAYS = 30;
    private static final int PRODUCT_FACT_LIMIT = 20;
    private static final int ORDER_FACT_LIMIT = 10;

    private final WalletRepository walletRepository;
    private final SchoolAssistantPaymentRepository paymentRepository;
    private final SchoolAssistantPurchaseRepository schoolAssistantPurchaseRepository;
    private final SchoolCalendarRepository schoolCalendarRepository;
    private final ProductRepository productRepository;
    private final PurchaseRepository purchaseRepository;
    private final PresupuestoRepository presupuestoRepository;
    private final SchoolAssistantRoleCatalog roleCatalog;
    private final Clock clock;

    SchoolAssistantContextFactory(
            WalletRepository walletRepository,
            SchoolAssistantPaymentRepository paymentRepository,
            SchoolAssistantPurchaseRepository schoolAssistantPurchaseRepository,
            SchoolCalendarRepository schoolCalendarRepository,
            ProductRepository productRepository,
            PurchaseRepository purchaseRepository,
            PresupuestoRepository presupuestoRepository,
            SchoolAssistantRoleCatalog roleCatalog,
            Clock clock
    ) {
        this.walletRepository = walletRepository;
        this.paymentRepository = paymentRepository;
        this.schoolAssistantPurchaseRepository = schoolAssistantPurchaseRepository;
        this.schoolCalendarRepository = schoolCalendarRepository;
        this.productRepository = productRepository;
        this.purchaseRepository = purchaseRepository;
        this.presupuestoRepository = presupuestoRepository;
        this.roleCatalog = roleCatalog;
        this.clock = clock;
    }

    SchoolAssistantContext studentContext(UUID studentId) {
        LocalDate today = LocalDate.now(clock);
        return new SchoolAssistantContext(
                studentId,
                walletRepository.findByStudentId(studentId).map(Billetera::getSaldoCreditos),
                paymentRepository.findRecentByStudentId(studentId, RECENT_PAYMENT_LIMIT),
                schoolAssistantPurchaseRepository.findRecentByStudentId(
                        studentId,
                        RECENT_PURCHASE_LIMIT
                ),
                schoolCalendarRepository.findByStudentIdAndDateRange(
                        studentId,
                        today,
                        today.plusDays(UPCOMING_EVENT_DAYS)
                ),
                SchoolAssistantRole.ALUMNO,
                null,
                null,
                List.of(),
                roleCatalog.capabilitiesFor(SchoolAssistantRole.ALUMNO)
        );
    }

    SchoolAssistantContext tutorContext(UUID userId, Tutor tutor) {
        List<Alumno> children = children(tutor);
        List<Purchase> purchases = purchaseRepository.findHistorialByTutor(tutor.getId());
        List<String> facts = new ArrayList<>();
        facts.add("tutorId=" + value(tutor.getId()));
        children.stream()
                .map(this::childFact)
                .forEach(facts::add);
        purchases.stream()
                .limit(RECENT_PURCHASE_LIMIT)
                .map(this::purchaseFact)
                .map(fact -> "compraTutor=" + fact)
                .forEach(facts::add);

        return new SchoolAssistantContext(
                null,
                Optional.empty(),
                tutorPayments(children),
                purchases.stream()
                        .limit(RECENT_PURCHASE_LIMIT)
                        .map(this::assistantPurchase)
                        .toList(),
                tutorEvents(children),
                SchoolAssistantRole.TUTOR,
                userId,
                null,
                facts,
                roleCatalog.capabilitiesFor(SchoolAssistantRole.TUTOR)
        );
    }

    SchoolAssistantContext kiosqueroContext(UUID userId, UUID buffetId) {
        List<String> facts = new ArrayList<>();
        facts.add("buffetId=" + buffetId);
        productRepository.findAllActiveWithStock(buffetId)
                .stream()
                .limit(PRODUCT_FACT_LIMIT)
                .map(this::productFact)
                .map(fact -> "productoBuffet=" + fact)
                .forEach(facts::add);
        List<Purchase> pendingOrders = purchaseRepository.findByBuffetAndStatuses(
                buffetId,
                List.of(EstadoCompra.PENDIENTE, EstadoCompra.EN_PREPARACION,
                        EstadoCompra.LISTO),
                ORDER_FACT_LIMIT
        );
        facts.add("pedidosPendientes=" + pendingOrders.size());
        pendingOrders.stream()
                .map(this::purchaseFact)
                .map(fact -> "pedido=" + fact)
                .forEach(facts::add);

        return new SchoolAssistantContext(
                null,
                Optional.empty(),
                List.of(),
                purchaseRepository.findRecentByBuffet(buffetId, RECENT_PURCHASE_LIMIT)
                        .stream()
                        .map(this::assistantPurchase)
                        .toList(),
                List.of(),
                SchoolAssistantRole.KIOSQUERO,
                userId,
                buffetId,
                facts,
                roleCatalog.capabilitiesFor(SchoolAssistantRole.KIOSQUERO)
        );
    }

    private List<Alumno> children(Tutor tutor) {
        Set<Alumno> children = tutor.getHijos();

        if (children == null) {
            List<Alumno> emptyChildren = List.of();
            return emptyChildren;
        }

        return children.stream().toList();
    }

    private String childFact(Alumno child) {
        return "hijo=" + name(child)
                + "|id=" + value(child.getId())
                + "|saldo=" + balance(child)
                + "|grado=" + grade(child)
                + "|presupuesto=" + budget(child)
                + "|restriccionesSalud=" + healthRestrictions(child)
                + "|productosBloqueados=" + blockedProducts(child);
    }

    private String productFact(Producto product) {
        return "id=" + value(product.getId())
                + "|nombre=" + value(product.getNombre())
                + "|precio=" + value(product.getPrecio())
                + "|stock=" + value(product.getStockDisponible())
                + "|requierePreparacion=" + value(product.getRequierePreparacion());
    }

    private String purchaseFact(Purchase purchase) {
        return "id=" + value(purchase.getId())
                + "|alumnoId=" + studentId(purchase)
                + "|monto=" + value(purchase.getTotalAmount())
                + "|estado=" + value(purchase.getStatus())
                + "|medioPago=" + value(purchase.getPaymentMethod())
                + "|fecha=" + value(purchase.getDate());
    }

    private SchoolAssistantPurchase assistantPurchase(Purchase purchase) {
        return new SchoolAssistantPurchase(
                purchase.getTotalAmount(),
                purchase.getStatus(),
                purchase.getPaymentMethod(),
                purchase.getDate()
        );
    }

    private List<SchoolCalendarEvent> tutorEvents(List<Alumno> children) {
        LocalDate today = LocalDate.now(clock);
        return children.stream()
                .flatMap(child -> schoolCalendarRepository.findByStudentIdAndDateRange(
                                child.getId(),
                                today,
                                today.plusDays(UPCOMING_EVENT_DAYS)
                        )
                        .stream())
                .toList();
    }

    private List<SchoolAssistantPayment> tutorPayments(List<Alumno> children) {
        return children.stream()
                .flatMap(child -> paymentRepository.findRecentByStudentId(
                                child.getId(),
                                CHILD_PAYMENT_LIMIT
                        )
                        .stream())
                .limit(RECENT_PAYMENT_LIMIT)
                .toList();
    }

    private String budget(Alumno child) {
        return presupuestoRepository.findActivoByAlumnoId(child.getId())
                .map(this::budgetFact)
                .orElse("sin_presupuesto");
    }

    private String budgetFact(PresupuestoAlumno budget) {
        return "monto=" + value(budget.getMontoLimiteGeneral())
                + ",periodo=" + value(budget.getPeriodo())
                + ",fechaInicio=" + value(budget.getFechaInicio());
    }

    private String name(Alumno child) {
        if (child.getUsuario() == null) {
            String missingName = "sin_datos";
            return missingName;
        }

        return value(child.getUsuario().getNombre()) + " "
                + value(child.getUsuario().getApellido());
    }

    private String balance(Alumno child) {
        return Optional.ofNullable(child.getBilletera())
                .map(Billetera::getSaldoCreditos)
                .map(BigDecimal::toString)
                .orElse("sin_datos");
    }

    private String grade(Alumno child) {
        if (child.getGrado() == null) {
            String missingGrade = "sin_datos";
            return missingGrade;
        }

        return value(child.getGrado().getAnio()) + " "
                + value(child.getGrado().getDivision());
    }

    private String healthRestrictions(Alumno child) {
        if (child.getRestriccionesSalud() == null) {
            String emptyRestrictions = "[]";
            return emptyRestrictions;
        }

        return child.getRestriccionesSalud()
                .stream()
                .map(restriction -> value(restriction.getDescripcion()))
                .toList()
                .toString();
    }

    private String blockedProducts(Alumno child) {
        if (child.getProductosBloqueados() == null) {
            String emptyProducts = "[]";
            return emptyProducts;
        }

        return child.getProductosBloqueados()
                .stream()
                .map(product -> value(product.getNombre()))
                .toList()
                .toString();
    }

    private String studentId(Purchase purchase) {
        if (purchase.getStudent() == null) {
            String missingStudent = "sin_datos";
            return missingStudent;
        }

        return value(purchase.getStudent().getId());
    }

    private String value(Object value) {
        return value == null ? "sin_datos" : value.toString();
    }
}
