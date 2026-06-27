package com.recred.backend.application;

import com.recred.backend.application.GetKiosqueroDashboardResponse.DashboardActivity;
import com.recred.backend.application.GetKiosqueroDashboardResponse.DashboardCategorySales;
import com.recred.backend.application.GetKiosqueroDashboardResponse.DashboardStatusOrders;
import com.recred.backend.application.GetKiosqueroDashboardResponse.DashboardTimeSlotSales;
import com.recred.backend.application.GetKiosqueroDashboardResponse.DashboardTypeOrders;
import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.TipoCompra;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

class DashboardActivityCalculator {

    private static final String SPONTANEOUS_SLOT = "Compra espontanea";
    private static final String UNCATEGORIZED = "Sin categoria";

    DashboardActivity calculate(List<Purchase> purchases) {
        return new DashboardActivity(
                salesByTimeSlot(purchases),
                salesByCategory(purchases),
                ordersByStatus(purchases),
                ordersByPurchaseType(purchases)
        );
    }

    private List<DashboardTimeSlotSales> salesByTimeSlot(List<Purchase> purchases) {
        Map<TimeSlotKey, TimeSlotAccumulator> slots = new LinkedHashMap<>();
        purchases.stream()
                .filter(Purchase::isDelivered)
                .forEach(purchase -> addTimeSlotSale(slots, purchase));
        return slots.values().stream()
                .map(TimeSlotAccumulator::toMetric)
                .sorted(timeSlotSalesComparator())
                .toList();
    }

    private void addTimeSlotSale(
            Map<TimeSlotKey, TimeSlotAccumulator> slots,
            Purchase purchase
    ) {
        slots.computeIfAbsent(timeSlotKey(purchase), TimeSlotAccumulator::new)
                .add(purchase.totalAmountOrZero());
    }

    private List<DashboardCategorySales> salesByCategory(List<Purchase> purchases) {
        Map<CategoryKey, CategoryAccumulator> categories = new LinkedHashMap<>();
        purchases.stream()
                .filter(Purchase::isDelivered)
                .forEach(purchase -> addCategories(categories, purchase));
        return categories.values().stream()
                .map(CategoryAccumulator::toMetric)
                .sorted(categorySalesComparator())
                .toList();
    }

    private List<DashboardStatusOrders> ordersByStatus(List<Purchase> purchases) {
        return Arrays.stream(EstadoCompra.values())
                .map(status -> new DashboardStatusOrders(
                        status.name(),
                        status.getLabel(),
                        countStatus(purchases, status)
                ))
                .toList();
    }

    private List<DashboardTypeOrders> ordersByPurchaseType(List<Purchase> purchases) {
        return Arrays.stream(TipoCompra.values())
                .map(type -> new DashboardTypeOrders(type.name(), countType(purchases, type)))
                .toList();
    }

    private void addCategories(
            Map<CategoryKey, CategoryAccumulator> categories,
            Purchase purchase
    ) {
        purchase.getItems().forEach((product, quantity) ->
                addCategory(categories, product, quantity));
    }

    private void addCategory(
            Map<CategoryKey, CategoryAccumulator> categories,
            Producto product,
            Integer quantity
    ) {
        if (invalidCategoryQuantity(product, quantity)) {
            return;
        }

        CategoryKey key = categoryKey(product);
        categories.computeIfAbsent(key, CategoryAccumulator::new)
                .add(quantity, product.priceOrZero());
    }

    private boolean invalidCategoryQuantity(Producto product, Integer quantity) {
        return product == null || invalidQuantity(quantity);
    }

    private boolean invalidQuantity(Integer quantity) {
        return quantity == null || quantity <= 0;
    }

    private CategoryKey categoryKey(Producto product) {
        if (product.getCategoria() == null) {
            return new CategoryKey(null, UNCATEGORIZED);
        }

        String categoryName = product.getCategoria().getDescripcion() == null
                ? UNCATEGORIZED
                : product.getCategoria().getDescripcion();
        return new CategoryKey(product.getCategoria().getId(), categoryName);
    }

    private TimeSlotKey timeSlotKey(Purchase purchase) {
        if (purchase.hasPickupSlotDescription()) {
            return new TimeSlotKey(
                    purchase.getPickupSlotId(),
                    purchase.getPickupSlotDescription(),
                    purchase.getPickupSlotStart(),
                    purchase.getPickupSlotEnd()
            );
        }

        return new TimeSlotKey(null, SPONTANEOUS_SLOT, null, null);
    }

    private Comparator<DashboardCategorySales> categorySalesComparator() {
        return Comparator
                .comparingInt(DashboardCategorySales::quantity)
                .reversed()
                .thenComparing(DashboardCategorySales::categoryName);
    }

    private Comparator<DashboardTimeSlotSales> timeSlotSalesComparator() {
        return Comparator
                .comparing((DashboardTimeSlotSales metric) ->
                        metric.pickupSlotStartTime() == null)
                .thenComparing(
                        DashboardTimeSlotSales::pickupSlotStartTime,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(DashboardTimeSlotSales::timeSlot);
    }

    private int countStatus(List<Purchase> purchases, EstadoCompra status) {
        return (int) purchases.stream()
                .filter(purchase -> purchase.hasStatus(status))
                .count();
    }

    private int countType(List<Purchase> purchases, TipoCompra type) {
        return (int) purchases.stream()
                .filter(purchase -> purchase.hasType(type))
                .count();
    }

    private static class TimeSlotAccumulator {
        private final TimeSlotKey timeSlot;
        private int orders;
        private BigDecimal totalSold = BigDecimal.ZERO;

        TimeSlotAccumulator(TimeSlotKey timeSlot) {
            this.timeSlot = timeSlot;
        }

        void add(BigDecimal amount) {
            orders++;
            totalSold = totalSold.add(amount);
        }

        DashboardTimeSlotSales toMetric() {
            return new DashboardTimeSlotSales(
                    timeSlot.id(),
                    timeSlot.name(),
                    timeSlot.startTime(),
                    timeSlot.endTime(),
                    orders,
                    totalSold
            );
        }
    }

    private static class CategoryAccumulator {
        private final CategoryKey category;
        private int quantity;
        private BigDecimal total = BigDecimal.ZERO;

        CategoryAccumulator(CategoryKey category) {
            this.category = category;
        }

        void add(int quantityToAdd, BigDecimal unitPrice) {
            quantity += quantityToAdd;
            total = total.add(unitPrice.multiply(BigDecimal.valueOf(quantityToAdd)));
        }

        DashboardCategorySales toMetric() {
            return new DashboardCategorySales(
                    category.id(),
                    category.name(),
                    quantity,
                    total
            );
        }
    }

    private record CategoryKey(UUID id, String name) {
    }

    private record TimeSlotKey(
            UUID id,
            String name,
            LocalTime startTime,
            LocalTime endTime
    ) {
    }
}
