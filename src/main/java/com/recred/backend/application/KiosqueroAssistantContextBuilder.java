package com.recred.backend.application;

import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.SchoolAssistantContext;
import com.recred.backend.domain.models.SchoolAssistantPurchase;
import com.recred.backend.domain.models.SchoolAssistantRole;
import com.recred.backend.domain.repositories.ProductRepository;
import com.recred.backend.domain.repositories.PurchaseRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class KiosqueroAssistantContextBuilder {

    private static final int RECENT_PURCHASE_LIMIT = 10;
    private static final int PRODUCT_FACT_LIMIT = 20;
    private static final int ORDER_FACT_LIMIT = 10;

    private final ProductRepository productRepository;
    private final PurchaseRepository purchaseRepository;
    private final SchoolAssistantRoleCatalog roleCatalog;

    public KiosqueroAssistantContextBuilder(
            ProductRepository productRepository,
            PurchaseRepository purchaseRepository,
            SchoolAssistantRoleCatalog roleCatalog
    ) {
        this.productRepository = productRepository;
        this.purchaseRepository = purchaseRepository;
        this.roleCatalog = roleCatalog;
    }

    public SchoolAssistantContext build(UUID userId, UUID buffetId) {
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
                facts(buffetId),
                roleCatalog.capabilitiesFor(SchoolAssistantRole.KIOSQUERO)
        );
    }

    private List<String> facts(UUID buffetId) {
        List<String> facts = new ArrayList<>();
        facts.add("buffetId=" + buffetId);
        productRepository.findAllActiveWithStock(buffetId)
                .stream()
                .limit(PRODUCT_FACT_LIMIT)
                .map(this::productFact)
                .map(fact -> "productoBuffet=" + fact)
                .forEach(facts::add);
        List<Purchase> pendingOrders = pendingOrders(buffetId);
        facts.add("pedidosPendientes=" + pendingOrders.size());
        pendingOrders.stream()
                .map(this::purchaseFact)
                .map(fact -> "pedido=" + fact)
                .forEach(facts::add);
        return facts;
    }

    private List<Purchase> pendingOrders(UUID buffetId) {
        return purchaseRepository.findByBuffetAndStatuses(
                buffetId,
                List.of(
                        EstadoCompra.PENDIENTE,
                        EstadoCompra.EN_PREPARACION,
                        EstadoCompra.LISTO
                ),
                ORDER_FACT_LIMIT
        );
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

    private String studentId(Purchase purchase) {
        if (purchase.getStudent() == null) {
            return "sin_datos";
        }

        return value(purchase.getStudent().getId());
    }

    private String value(Object value) {
        return value == null ? "sin_datos" : value.toString();
    }
}
