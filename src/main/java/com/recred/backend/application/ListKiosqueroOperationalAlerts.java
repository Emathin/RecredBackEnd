package com.recred.backend.application;

import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.KiosqueroOperationalAlert;
import com.recred.backend.domain.models.OperationalAlertSeverity;
import com.recred.backend.domain.models.OperationalAlertType;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.repositories.ProductRepository;
import com.recred.backend.domain.repositories.PurchaseRepository;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ListKiosqueroOperationalAlerts {

    private static final int MANY_PENDING_THRESHOLD = 5;
    private static final int ACCUMULATION_THRESHOLD = 8;
    private static final int UPCOMING_PICKUP_MINUTES = 15;
    private static final List<EstadoCompra> ACTIVE_STATUSES = List.of(
            EstadoCompra.PENDIENTE,
            EstadoCompra.EN_PREPARACION,
            EstadoCompra.LISTO
    );

    private final PurchaseRepository purchaseRepository;
    private final ProductRepository productRepository;
    private final Clock clock;

    public ListKiosqueroOperationalAlerts(
            PurchaseRepository purchaseRepository,
            ProductRepository productRepository,
            Clock clock
    ) {
        this.purchaseRepository = purchaseRepository;
        this.productRepository = productRepository;
        this.clock = clock;
    }

    public ListKiosqueroOperationalAlertsResponse execute(
            ListKiosqueroOperationalAlertsRequest request
    ) {
        return new ListKiosqueroOperationalAlertsResponse(alerts(request.buffetId()).stream()
                .map(OperationalAlertResponse::fromDomain)
                .toList());
    }

    private List<KiosqueroOperationalAlert> alerts(UUID buffetId) {
        LocalDateTime now = LocalDateTime.now(clock);
        List<Purchase> activeOrders = purchaseRepository.findByBuffetAndStatuses(
                buffetId,
                ACTIVE_STATUSES,
                100
        );
        Map<UUID, Producto> products = productRepository.findAllActiveWithStock(buffetId)
                .stream()
                .collect(Collectors.toMap(Producto::getId, Function.identity()));
        List<KiosqueroOperationalAlert> alerts = new ArrayList<>();
        addPendingAlerts(buffetId, now, activeOrders, alerts);
        addPickupAlerts(buffetId, now, activeOrders, alerts);
        addDailyCapacityAlerts(buffetId, now, products, alerts);
        return alerts;
    }

    private void addPendingAlerts(
            UUID buffetId,
            LocalDateTime now,
            List<Purchase> orders,
            List<KiosqueroOperationalAlert> alerts
    ) {
        long pending = orders.stream()
                .filter(Purchase::isPending)
                .count();
        if (pending >= MANY_PENDING_THRESHOLD) {
            alerts.add(alert(
                    buffetId,
                    OperationalAlertType.MANY_PENDING_ORDERS,
                    OperationalAlertSeverity.WARNING,
                    null,
                    null,
                    now,
                    "Hay " + pending + " pedidos pendientes"
            ));
        }

        if (orders.size() >= ACCUMULATION_THRESHOLD) {
            alerts.add(alert(
                    buffetId,
                    OperationalAlertType.ORDER_ACCUMULATION,
                    OperationalAlertSeverity.WARNING,
                    null,
                    null,
                    now,
                    "Hay " + orders.size() + " pedidos activos acumulados"
            ));
        }
    }

    private void addPickupAlerts(
            UUID buffetId,
            LocalDateTime now,
            List<Purchase> orders,
            List<KiosqueroOperationalAlert> alerts
    ) {
        LocalDate today = now.toLocalDate();
        orders.stream()
                .filter(order -> today.equals(order.getPickupDate()))
                .forEach(order -> addPickupAlert(buffetId, now, order, alerts));
    }

    private void addPickupAlert(
            UUID buffetId,
            LocalDateTime now,
            Purchase order,
            List<KiosqueroOperationalAlert> alerts
    ) {
        if (order.getPickupSlotEnd() != null
                && now.toLocalTime().isAfter(order.getPickupSlotEnd())) {
            alerts.add(alert(
                    buffetId,
                    OperationalAlertType.ORDER_EXPIRED,
                    OperationalAlertSeverity.CRITICAL,
                    null,
                    order.getId(),
                    now,
                    "Pedido no retirado dentro de la franja"
            ));
            return;
        }

        if (order.getPickupSlotStart() == null) {
            return;
        }

        LocalDateTime pickupStart = LocalDateTime.of(order.getPickupDate(),
                order.getPickupSlotStart());
        if (!pickupStart.isBefore(now)
                && !pickupStart.isAfter(now.plusMinutes(UPCOMING_PICKUP_MINUTES))) {
            alerts.add(alert(
                    buffetId,
                    OperationalAlertType.ORDER_UPCOMING_PICKUP,
                    OperationalAlertSeverity.INFO,
                    null,
                    order.getId(),
                    now,
                    "Pedido proximo a retirar"
            ));
        }
    }

    private void addDailyCapacityAlerts(
            UUID buffetId,
            LocalDateTime now,
            Map<UUID, Producto> products,
            List<KiosqueroOperationalAlert> alerts
    ) {
        products.values().stream()
                .filter(Producto::hasDailyCapacityLimit)
                .forEach(product -> addDailyCapacityAlert(buffetId, now, product, alerts));
    }

    private void addDailyCapacityAlert(
            UUID buffetId,
            LocalDateTime now,
            Producto product,
            List<KiosqueroOperationalAlert> alerts
    ) {
        long reserved = purchaseRepository.sumScheduledProductQuantity(
                buffetId,
                product.getId(),
                now.toLocalDate(),
                ACTIVE_STATUSES
        );
        int remaining = product.getCupoMaximoDiario() - (int) reserved;
        product.applyDailyCapacity(reserved);
        if (remaining <= Math.max(1, product.getCupoMaximoDiario() / 5)) {
            alerts.add(alert(
                    buffetId,
                    OperationalAlertType.DAILY_CAPACITY_LOW,
                    OperationalAlertSeverity.WARNING,
                    product.getId(),
                    null,
                    now,
                    "Cupo diario bajo para " + product.getNombre()
            ));
        }
    }

    private KiosqueroOperationalAlert alert(
            UUID buffetId,
            OperationalAlertType type,
            OperationalAlertSeverity severity,
            UUID productId,
            UUID purchaseId,
            LocalDateTime now,
            String message
    ) {
        return new KiosqueroOperationalAlert(
                buffetId,
                type,
                severity,
                productId,
                purchaseId,
                now,
                message
        );
    }
}
