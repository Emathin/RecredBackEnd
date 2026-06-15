package com.recred.backend.application;

import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.EstadoInventario;
import com.recred.backend.domain.models.KiosqueroDashboard;
import com.recred.backend.domain.models.KiosqueroDashboard.DashboardActivity;
import com.recred.backend.domain.models.KiosqueroDashboard.DashboardAlert;
import com.recred.backend.domain.models.KiosqueroDashboard.DashboardAlerts;
import com.recred.backend.domain.models.KiosqueroDashboard.DashboardCategorySales;
import com.recred.backend.domain.models.KiosqueroDashboard.DashboardInventoryAttention;
import com.recred.backend.domain.models.KiosqueroDashboard.DashboardProductMetric;
import com.recred.backend.domain.models.KiosqueroDashboard.DashboardProducts;
import com.recred.backend.domain.models.KiosqueroDashboard.DashboardStatusOrders;
import com.recred.backend.domain.models.KiosqueroDashboard.DashboardSummary;
import com.recred.backend.domain.models.KiosqueroDashboard.DashboardTimeSlotSales;
import com.recred.backend.domain.models.KiosqueroDashboard.DashboardTrendPoint;
import com.recred.backend.domain.models.KiosqueroDashboard.DashboardTrends;
import com.recred.backend.domain.models.KiosqueroDashboard.DashboardTypeOrders;
import com.recred.backend.domain.models.MovimientoStock;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.TipoCompra;
import com.recred.backend.domain.models.TipoMovimientoStock;
import com.recred.backend.domain.repositories.ProductRepository;
import com.recred.backend.domain.repositories.PurchaseRepository;
import com.recred.backend.domain.repositories.StockMovementRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class GetKiosqueroDashboard {

    private static final int TOP_PRODUCTS_LIMIT = 5;
    private static final int TREND_DAYS = 7;
    private static final int MAX_DASHBOARD_RANGE_DAYS = 366;
    private static final String SPONTANEOUS_SLOT = "Compra espontanea";
    private static final String UNCATEGORIZED = "Sin categoria";
    private static final String ALERT_EXPIRED_ORDERS = "EXPIRED_ORDERS";
    private static final String ALERT_RELEASED_RESERVATIONS = "RELEASED_RESERVATIONS";
    private static final String ALERT_REFUNDED_CREDITS = "REFUNDED_CREDITS";
    private static final String ALERT_SOLD_OUT_PRODUCTS = "SOLD_OUT_PRODUCTS";
    private static final String ALERT_PENDING_ORDERS = "PENDING_ORDERS";
    private static final String ALERT_READY_ORDERS = "READY_ORDERS";

    private final PurchaseRepository purchaseRepository;
    private final ProductRepository productRepository;
    private final StockMovementRepository stockMovementRepository;
    private final Clock clock;

    public GetKiosqueroDashboard(
            PurchaseRepository purchaseRepository,
            ProductRepository productRepository,
            StockMovementRepository stockMovementRepository,
            Clock clock
    ) {
        this.purchaseRepository = purchaseRepository;
        this.productRepository = productRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.clock = clock;
    }

    public KiosqueroDashboard execute(UUID buffetId, LocalDate date) {
        return execute(buffetId, date, null, null);
    }

    public KiosqueroDashboard execute(
            UUID buffetId,
            LocalDate date,
            LocalDate from,
            LocalDate to
    ) {
        if (buffetId == null) {
            throw new IllegalArgumentException("buffetId is required");
        }

        DashboardPeriod period = dashboardPeriod(date, from, to);
        DateTimeRange purchaseRange = range(period.trendStart(), period.trendEnd());
        DateTimeRange movementRange = range(period.metricStart(), period.metricEnd());
        List<Purchase> purchases = purchaseRepository.findByBuffetAndDateRange(
                buffetId,
                purchaseRange.start(),
                purchaseRange.end()
        );
        List<Purchase> metricPurchases = purchasesOfPeriod(
                purchases,
                period.metricStart(),
                period.metricEnd()
        );
        List<Producto> inventory = productRepository.findAllActiveWithStock(buffetId)
                .stream()
                .filter(this::hasInventory)
                .toList();
        List<MovimientoStock> movements = stockMovementRepository.findByBuffetAndDateRange(
                buffetId,
                movementRange.start(),
                movementRange.end()
        );

        return new KiosqueroDashboard(
                buffetId,
                period.responseDate(),
                summary(metricPurchases, inventory),
                activity(metricPurchases),
                products(metricPurchases, inventory, period.rangeMode()),
                alerts(metricPurchases, movements),
                trends(period.trendStart(), period.trendEnd(), purchases)
        );
    }

    private LocalDate requestedDate(LocalDate date) {
        return date == null ? LocalDate.now(clock) : date;
    }

    private DashboardPeriod dashboardPeriod(
            LocalDate date,
            LocalDate from,
            LocalDate to
    ) {
        if (from == null && to == null) {
            LocalDate requestedDate = requestedDate(date);
            return new DashboardPeriod(
                    requestedDate,
                    requestedDate,
                    requestedDate,
                    requestedDate.minusDays(TREND_DAYS - 1L),
                    requestedDate,
                    false
            );
        }

        validateDateRange(from, to);
        return new DashboardPeriod(to, from, to, from, to, true);
    }

    private void validateDateRange(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("from and to are required together");
        }

        if (from.isAfter(to)) {
            throw new IllegalArgumentException("from must be before or equal to to");
        }

        if (daysBetweenInclusive(from, to) > MAX_DASHBOARD_RANGE_DAYS) {
            throw new IllegalArgumentException("dashboard range cannot exceed 366 days");
        }
    }

    private DateTimeRange range(LocalDate startDate, LocalDate endDate) {
        return new DateTimeRange(
                startDate.atStartOfDay(),
                endDate.plusDays(1).atStartOfDay().minusNanos(1)
        );
    }

    private List<Purchase> purchasesOfPeriod(
            List<Purchase> purchases,
            LocalDate from,
            LocalDate to
    ) {
        return purchases.stream()
                .filter(purchase -> happenedBetween(purchase, from, to))
                .toList();
    }

    private boolean happenedBetween(Purchase purchase, LocalDate from, LocalDate to) {
        if (purchase.getDate() == null) {
            return false;
        }

        LocalDate purchaseDate = purchase.getDate().toLocalDate();
        return !purchaseDate.isBefore(from) && !purchaseDate.isAfter(to);
    }

    private DashboardSummary summary(
            List<Purchase> purchases,
            List<Producto> inventory
    ) {
        BigDecimal totalSold = totalSold(purchases);
        int deliveredOrders = countStatus(purchases, EstadoCompra.ENTREGADO);
        return new DashboardSummary(
                totalSold,
                purchases.size(),
                deliveredOrders,
                averageTicket(totalSold, deliveredOrders),
                countStatus(purchases, EstadoCompra.PENDIENTE),
                soldOutProductsCount(inventory)
        );
    }

    private DashboardActivity activity(List<Purchase> purchases) {
        return new DashboardActivity(
                salesByTimeSlot(purchases),
                salesByCategory(purchases),
                ordersByStatus(purchases),
                ordersByPurchaseType(purchases)
        );
    }

    private DashboardProducts products(
            List<Purchase> purchases,
            List<Producto> inventory,
            boolean rangeMode
    ) {
        return new DashboardProducts(
                topSoldProducts(purchases),
                mostReservedProducts(purchases, inventory, rangeMode),
                productsNeedingRestock(inventory),
                soldOutProducts(inventory)
        );
    }

    private DashboardAlerts alerts(
            List<Purchase> purchases,
            List<MovimientoStock> movements
    ) {
        int expiredOrders = countStatus(purchases, EstadoCompra.VENCIDO);
        int releasedReservations = releasedReservations(movements);
        BigDecimal refundedCredits = refundedCredits(purchases);
        int soldOutEvents = soldOutEvents(movements);
        int pendingOrders = countStatus(purchases, EstadoCompra.PENDIENTE);
        int readyOrders = countStatus(purchases, EstadoCompra.LISTO);

        return new DashboardAlerts(
                expiredOrders,
                releasedReservations,
                refundedCredits,
                soldOutEvents,
                pendingOrders,
                readyOrders,
                alertItems(
                        expiredOrders,
                        releasedReservations,
                        refundedCredits,
                        soldOutEvents,
                        pendingOrders,
                        readyOrders
                )
        );
    }

    private DashboardTrends trends(
            LocalDate startDate,
            LocalDate endDate,
            List<Purchase> purchases
    ) {
        Map<LocalDate, List<Purchase>> purchasesByDate = purchases.stream()
                .filter(purchase -> purchase.getDate() != null)
                .collect(Collectors.groupingBy(purchase -> purchase.getDate().toLocalDate()));
        List<DashboardTrendPoint> points = IntStream.range(
                        0,
                        daysBetweenInclusive(startDate, endDate)
                )
                .mapToObj(index -> trendPoint(
                        startDate.plusDays(index),
                        purchasesByDate
                ))
                .toList();
        return new DashboardTrends(points);
    }

    private DashboardTrendPoint trendPoint(
            LocalDate date,
            Map<LocalDate, List<Purchase>> purchasesByDate
    ) {
        List<Purchase> purchases = purchasesByDate.getOrDefault(date, List.of());
        return new DashboardTrendPoint(
                date,
                totalSold(purchases),
                purchases.size(),
                countStatus(purchases, EstadoCompra.ENTREGADO),
                purchases.size(),
                countStatus(purchases, EstadoCompra.PENDIENTE),
                countStatus(purchases, EstadoCompra.EN_PREPARACION),
                countStatus(purchases, EstadoCompra.LISTO),
                countStatus(purchases, EstadoCompra.CANCELADO),
                countStatus(purchases, EstadoCompra.RECHAZADO),
                countStatus(purchases, EstadoCompra.VENCIDO)
        );
    }

    private List<DashboardTimeSlotSales> salesByTimeSlot(List<Purchase> purchases) {
        Map<TimeSlotKey, TimeSlotAccumulator> slots = new LinkedHashMap<>();
        purchases.stream()
                .filter(this::deliveredPurchase)
                .forEach(purchase -> slots
                        .computeIfAbsent(timeSlotKey(purchase), TimeSlotAccumulator::new)
                        .add(amount(purchase)));
        return slots.values().stream()
                .map(TimeSlotAccumulator::toMetric)
                .sorted(timeSlotSalesComparator())
                .toList();
    }

    private List<DashboardCategorySales> salesByCategory(List<Purchase> purchases) {
        Map<CategoryKey, CategoryAccumulator> categories = new LinkedHashMap<>();
        purchases.stream()
                .filter(this::deliveredPurchase)
                .forEach(purchase -> addCategories(categories, purchase));
        return categories.values().stream()
                .map(CategoryAccumulator::toMetric)
                .sorted(categorySalesComparator())
                .toList();
    }

    private List<DashboardStatusOrders> ordersByStatus(List<Purchase> purchases) {
        return Arrays.stream(EstadoCompra.values())
                .map(status -> new DashboardStatusOrders(
                        status,
                        status.getLabel(),
                        countStatus(purchases, status)
                ))
                .toList();
    }

    private List<DashboardTypeOrders> ordersByPurchaseType(List<Purchase> purchases) {
        return Arrays.stream(TipoCompra.values())
                .map(type -> new DashboardTypeOrders(type, countType(purchases, type)))
                .toList();
    }

    private List<DashboardProductMetric> topSoldProducts(List<Purchase> purchases) {
        Map<UUID, ProductAccumulator> products = new LinkedHashMap<>();
        purchases.stream()
                .filter(this::deliveredPurchase)
                .forEach(purchase -> addProducts(products, purchase));
        return products.values().stream()
                .map(ProductAccumulator::toMetric)
                .sorted(productMetricComparator())
                .limit(TOP_PRODUCTS_LIMIT)
                .toList();
    }

    private List<DashboardProductMetric> mostReservedProducts(
            List<Purchase> purchases,
            List<Producto> inventory,
            boolean rangeMode
    ) {
        if (rangeMode) {
            return mostReservedPurchaseProducts(purchases);
        }

        return mostReservedInventoryProducts(inventory);
    }

    private List<DashboardProductMetric> mostReservedInventoryProducts(List<Producto> inventory) {
        return inventory.stream()
                .filter(product -> safeQuantity(product.getStockReservado()) > 0)
                .map(product -> new DashboardProductMetric(
                        product.getId(),
                        product.getNombre(),
                        safeQuantity(product.getStockReservado()),
                        BigDecimal.ZERO
                ))
                .sorted(productMetricComparator())
                .limit(TOP_PRODUCTS_LIMIT)
                .toList();
    }

    private List<DashboardProductMetric> mostReservedPurchaseProducts(List<Purchase> purchases) {
        Map<UUID, ProductAccumulator> products = new LinkedHashMap<>();
        purchases.stream()
                .filter(this::advancePurchase)
                .forEach(purchase -> addProducts(products, purchase));
        return products.values().stream()
                .map(ProductAccumulator::toMetric)
                .sorted(productMetricComparator())
                .limit(TOP_PRODUCTS_LIMIT)
                .toList();
    }

    private List<DashboardInventoryAttention> productsNeedingRestock(
            List<Producto> inventory
    ) {
        return inventory.stream()
                .filter(this::needsRestock)
                .map(this::toInventoryAttention)
                .sorted(inventoryAttentionComparator())
                .limit(TOP_PRODUCTS_LIMIT)
                .toList();
    }

    private List<DashboardInventoryAttention> soldOutProducts(List<Producto> inventory) {
        return inventory.stream()
                .filter(this::isSoldOut)
                .map(this::toInventoryAttention)
                .sorted(inventoryAttentionComparator())
                .limit(TOP_PRODUCTS_LIMIT)
                .toList();
    }

    private int soldOutProductsCount(List<Producto> inventory) {
        return (int) inventory.stream()
                .filter(this::isSoldOut)
                .count();
    }

    private Comparator<DashboardProductMetric> productMetricComparator() {
        return Comparator
                .comparingInt(DashboardProductMetric::quantity)
                .reversed()
                .thenComparing(DashboardProductMetric::productName);
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

    private Comparator<DashboardInventoryAttention> inventoryAttentionComparator() {
        return Comparator
                .comparingInt((DashboardInventoryAttention item) ->
                        safeQuantity(item.stockDisponible()))
                .thenComparing(DashboardInventoryAttention::productName);
    }

    private void addProducts(
            Map<UUID, ProductAccumulator> products,
            Purchase purchase
    ) {
        purchase.getItems().forEach((product, quantity) ->
                addProduct(products, product, quantity));
    }

    private void addCategories(
            Map<CategoryKey, CategoryAccumulator> categories,
            Purchase purchase
    ) {
        purchase.getItems().forEach((product, quantity) ->
                addCategory(categories, product, quantity));
    }

    private void addProduct(
            Map<UUID, ProductAccumulator> products,
            Producto product,
            Integer quantity
    ) {
        if (product == null || product.getId() == null || quantity == null || quantity <= 0) {
            return;
        }

        products.computeIfAbsent(
                product.getId(),
                id -> new ProductAccumulator(product)
        ).add(quantity, product.getPrecio());
    }

    private void addCategory(
            Map<CategoryKey, CategoryAccumulator> categories,
            Producto product,
            Integer quantity
    ) {
        if (product == null || quantity == null || quantity <= 0) {
            return;
        }

        CategoryKey key = categoryKey(product);
        categories.computeIfAbsent(key, CategoryAccumulator::new)
                .add(quantity, product.getPrecio());
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

    private List<DashboardAlert> alertItems(
            int expiredOrders,
            int releasedReservations,
            BigDecimal refundedCredits,
            int soldOutEvents,
            int pendingOrders,
            int readyOrders
    ) {
        List<DashboardAlert> items = new ArrayList<>();
        addQuantityAlert(items, ALERT_EXPIRED_ORDERS, "Pedidos vencidos", expiredOrders);
        addQuantityAlert(
                items,
                ALERT_RELEASED_RESERVATIONS,
                "Reservas liberadas",
                releasedReservations
        );
        addAmountAlert(items, ALERT_REFUNDED_CREDITS, "Creditos devueltos", refundedCredits);
        addQuantityAlert(items, ALERT_SOLD_OUT_PRODUCTS, "Productos agotados", soldOutEvents);
        addQuantityAlert(items, ALERT_PENDING_ORDERS, "Pedidos pendientes", pendingOrders);
        addQuantityAlert(items, ALERT_READY_ORDERS, "Pedidos listos sin entregar", readyOrders);
        return items;
    }

    private void addQuantityAlert(
            List<DashboardAlert> items,
            String type,
            String label,
            int quantity
    ) {
        if (quantity <= 0) {
            return;
        }

        items.add(new DashboardAlert(type, label, quantity, BigDecimal.ZERO));
    }

    private void addAmountAlert(
            List<DashboardAlert> items,
            String type,
            String label,
            BigDecimal amount
    ) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        items.add(new DashboardAlert(type, label, 0, amount));
    }

    private BigDecimal averageTicket(BigDecimal totalSold, int deliveredOrders) {
        if (deliveredOrders <= 0) {
            return BigDecimal.ZERO;
        }

        return totalSold.divide(BigDecimal.valueOf(deliveredOrders), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal totalSold(List<Purchase> purchases) {
        return purchases.stream()
                .filter(this::deliveredPurchase)
                .map(this::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal refundedCredits(List<Purchase> purchases) {
        return purchases.stream()
                .filter(this::refundedPurchase)
                .map(this::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private int releasedReservations(List<MovimientoStock> movements) {
        return movements.stream()
                .filter(movement -> movement.tipo() == TipoMovimientoStock.LIBERACION)
                .map(MovimientoStock::cantidad)
                .filter(Objects::nonNull)
                .reduce(0, Integer::sum);
    }

    private int soldOutEvents(List<MovimientoStock> movements) {
        return (int) movements.stream()
                .filter(this::stockReachedZero)
                .map(MovimientoStock::inventarioId)
                .filter(Objects::nonNull)
                .distinct()
                .count();
    }

    private boolean stockReachedZero(MovimientoStock movement) {
        return movement.cantidadNueva() != null && movement.cantidadNueva() <= 0;
    }

    private int countStatus(List<Purchase> purchases, EstadoCompra status) {
        return (int) purchases.stream()
                .filter(purchase -> purchase.getStatus() == status)
                .count();
    }

    private int countType(List<Purchase> purchases, TipoCompra type) {
        return (int) purchases.stream()
                .filter(purchase -> purchase.getTipo() == type)
                .count();
    }

    private boolean deliveredPurchase(Purchase purchase) {
        return purchase.getStatus() == EstadoCompra.ENTREGADO;
    }

    private boolean advancePurchase(Purchase purchase) {
        return purchase.getTipo() == TipoCompra.ANTICIPADA;
    }

    private boolean refundedPurchase(Purchase purchase) {
        return purchase.getStatus() == EstadoCompra.CANCELADO
                || purchase.getStatus() == EstadoCompra.RECHAZADO;
    }

    private TimeSlotKey timeSlotKey(Purchase purchase) {
        if (purchase.getPickupSlotDescription() != null
                && !purchase.getPickupSlotDescription().isBlank()) {
            return new TimeSlotKey(
                    purchase.getPickupSlotId(),
                    purchase.getPickupSlotDescription(),
                    purchase.getPickupSlotStart(),
                    purchase.getPickupSlotEnd()
            );
        }

        return new TimeSlotKey(null, SPONTANEOUS_SLOT, null, null);
    }

    private boolean hasInventory(Producto product) {
        return product.getStockActual() != null
                || product.getStockReservado() != null
                || product.getStockMinimo() != null
                || product.getCupoMaximoDiario() != null
                || product.getEstadoInventario() != null
                || product.getTipoManejoInventario() != null;
    }

    private boolean needsRestock(Producto product) {
        return isSoldOut(product)
                || product.getEstadoInventario() == EstadoInventario.BAJO_STOCK
                || hasLowStock(product);
    }

    private boolean hasLowStock(Producto product) {
        return product.getStockDisponible() != null
                && product.getStockMinimo() != null
                && product.getStockDisponible() <= product.getStockMinimo();
    }

    private boolean isSoldOut(Producto product) {
        if (product.getEstadoInventario() == EstadoInventario.SIN_STOCK
                || product.getEstadoInventario() == EstadoInventario.DESACTIVADO) {
            return true;
        }

        return product.getStockDisponible() != null && product.getStockDisponible() <= 0;
    }

    private DashboardInventoryAttention toInventoryAttention(Producto product) {
        return new DashboardInventoryAttention(
                product.getId(),
                product.getNombre(),
                product.getStockDisponible(),
                product.getStockMinimo(),
                product.getEstadoInventario() == null
                        ? null
                        : product.getEstadoInventario().name()
        );
    }

    private BigDecimal amount(Purchase purchase) {
        return purchase.getTotalAmount() == null ? BigDecimal.ZERO : purchase.getTotalAmount();
    }

    private int safeQuantity(Integer quantity) {
        return quantity == null ? 0 : quantity;
    }

    private int daysBetweenInclusive(LocalDate from, LocalDate to) {
        return Math.toIntExact(ChronoUnit.DAYS.between(from, to) + 1);
    }

    private static class ProductAccumulator {
        private final UUID productId;
        private final String productName;
        private int quantity;
        private BigDecimal total = BigDecimal.ZERO;

        ProductAccumulator(Producto product) {
            this.productId = product.getId();
            this.productName = product.getNombre();
        }

        void add(int quantityToAdd, BigDecimal unitPrice) {
            quantity += quantityToAdd;
            total = total.add(price(unitPrice).multiply(BigDecimal.valueOf(quantityToAdd)));
        }

        DashboardProductMetric toMetric() {
            return new DashboardProductMetric(productId, productName, quantity, total);
        }

        private BigDecimal price(BigDecimal value) {
            return value == null ? BigDecimal.ZERO : value;
        }
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
            total = total.add(price(unitPrice).multiply(BigDecimal.valueOf(quantityToAdd)));
        }

        DashboardCategorySales toMetric() {
            return new DashboardCategorySales(
                    category.id(),
                    category.name(),
                    quantity,
                    total
            );
        }

        private BigDecimal price(BigDecimal value) {
            return value == null ? BigDecimal.ZERO : value;
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

    private record DateTimeRange(LocalDateTime start, LocalDateTime end) {
    }

    private record DashboardPeriod(
            LocalDate responseDate,
            LocalDate metricStart,
            LocalDate metricEnd,
            LocalDate trendStart,
            LocalDate trendEnd,
            boolean rangeMode
    ) {
    }
}
