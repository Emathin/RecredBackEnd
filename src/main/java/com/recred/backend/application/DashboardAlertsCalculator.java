package com.recred.backend.application;

import com.recred.backend.application.GetKiosqueroDashboardResponse.DashboardAlert;
import com.recred.backend.application.GetKiosqueroDashboardResponse.DashboardAlerts;
import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.MovimientoStock;
import com.recred.backend.domain.models.Purchase;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

class DashboardAlertsCalculator {

    DashboardAlerts calculate(
            List<Purchase> purchases,
            List<MovimientoStock> movements
    ) {
        DashboardAlertTotals totals = alertTotals(purchases, movements);
        return new DashboardAlerts(
                totals.expiredOrders(),
                totals.releasedReservations(),
                totals.refundedCredits(),
                totals.soldOutEvents(),
                totals.pendingOrders(),
                totals.readyOrders(),
                alertItems(totals)
        );
    }

    private DashboardAlertTotals alertTotals(
            List<Purchase> purchases,
            List<MovimientoStock> movements
    ) {
        return new DashboardAlertTotals(
                countStatus(purchases, EstadoCompra.VENCIDO),
                releasedReservations(movements),
                refundedCredits(purchases),
                soldOutEvents(movements),
                countStatus(purchases, EstadoCompra.PENDIENTE),
                countStatus(purchases, EstadoCompra.LISTO)
        );
    }

    private List<DashboardAlert> alertItems(DashboardAlertTotals totals) {
        List<DashboardAlert> items = new ArrayList<>();
        addQuantityAlert(items, AlertDefinition.EXPIRED_ORDERS, totals.expiredOrders());
        addQuantityAlert(
                items,
                AlertDefinition.RELEASED_RESERVATIONS,
                totals.releasedReservations()
        );
        addAmountAlert(items, AlertDefinition.REFUNDED_CREDITS, totals.refundedCredits());
        addQuantityAlert(items, AlertDefinition.SOLD_OUT_PRODUCTS, totals.soldOutEvents());
        addQuantityAlert(items, AlertDefinition.PENDING_ORDERS, totals.pendingOrders());
        addQuantityAlert(items, AlertDefinition.READY_ORDERS, totals.readyOrders());
        return items;
    }

    private void addQuantityAlert(
            List<DashboardAlert> items,
            AlertDefinition definition,
            int quantity
    ) {
        if (quantity <= 0) {
            return;
        }

        items.add(new DashboardAlert(
                definition.type(),
                definition.label(),
                quantity,
                BigDecimal.ZERO
        ));
    }

    private void addAmountAlert(
            List<DashboardAlert> items,
            AlertDefinition definition,
            BigDecimal amount
    ) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        items.add(new DashboardAlert(
                definition.type(),
                definition.label(),
                0,
                amount
        ));
    }

    private BigDecimal refundedCredits(List<Purchase> purchases) {
        return purchases.stream()
                .filter(Purchase::refundsCredits)
                .map(Purchase::totalAmountOrZero)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private int releasedReservations(List<MovimientoStock> movements) {
        return movements.stream()
                .filter(MovimientoStock::isRelease)
                .map(MovimientoStock::quantityOrZero)
                .reduce(0, Integer::sum);
    }

    private int soldOutEvents(List<MovimientoStock> movements) {
        return (int) movements.stream()
                .filter(MovimientoStock::reachedZeroStock)
                .filter(MovimientoStock::hasInventoryId)
                .map(MovimientoStock::inventarioId)
                .distinct()
                .count();
    }

    private int countStatus(List<Purchase> purchases, EstadoCompra status) {
        return (int) purchases.stream()
                .filter(purchase -> purchase.hasStatus(status))
                .count();
    }

    private enum AlertDefinition {
        EXPIRED_ORDERS("EXPIRED_ORDERS", "Pedidos vencidos"),
        RELEASED_RESERVATIONS("RELEASED_RESERVATIONS", "Reservas liberadas"),
        REFUNDED_CREDITS("REFUNDED_CREDITS", "Creditos devueltos"),
        SOLD_OUT_PRODUCTS("SOLD_OUT_PRODUCTS", "Productos agotados"),
        PENDING_ORDERS("PENDING_ORDERS", "Pedidos pendientes"),
        READY_ORDERS("READY_ORDERS", "Pedidos listos sin entregar");

        private final String type;
        private final String label;

        AlertDefinition(String type, String label) {
            this.type = type;
            this.label = label;
        }

        String type() {
            return type;
        }

        String label() {
            return label;
        }
    }

    private record DashboardAlertTotals(
            int expiredOrders,
            int releasedReservations,
            BigDecimal refundedCredits,
            int soldOutEvents,
            int pendingOrders,
            int readyOrders
    ) {
    }
}
