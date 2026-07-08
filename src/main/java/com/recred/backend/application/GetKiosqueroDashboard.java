package com.recred.backend.application;

import com.recred.backend.application.GetKiosqueroDashboardResponse.DashboardActivity;
import com.recred.backend.application.GetKiosqueroDashboardResponse.DashboardAlerts;
import com.recred.backend.application.GetKiosqueroDashboardResponse.DashboardProducts;
import com.recred.backend.application.GetKiosqueroDashboardResponse.DashboardSummary;
import com.recred.backend.application.GetKiosqueroDashboardResponse.DashboardTrendPoint;
import com.recred.backend.application.GetKiosqueroDashboardResponse.DashboardTrends;
import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.MovimientoStock;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.repositories.ProductRepository;
import com.recred.backend.domain.repositories.PurchaseRepository;
import com.recred.backend.domain.repositories.StockMovementRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class GetKiosqueroDashboard {

    private static final int TREND_DAYS = 7;
    private static final int MAX_DASHBOARD_RANGE_DAYS = 366;

    private final PurchaseRepository purchaseRepository;
    private final ProductRepository productRepository;
    private final StockMovementRepository stockMovementRepository;
    private final DashboardActivityCalculator activityCalculator;
    private final DashboardProductsCalculator productsCalculator;
    private final DashboardAlertsCalculator alertsCalculator;
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
        this.activityCalculator = new DashboardActivityCalculator();
        this.productsCalculator = new DashboardProductsCalculator();
        this.alertsCalculator = new DashboardAlertsCalculator();
        this.clock = clock;
    }

    public GetKiosqueroDashboardResponse execute(GetKiosqueroDashboardRequest request) {
        requireRequest(request);
        DashboardPeriod period = dashboardPeriod(request);
        DashboardData data = dashboardData(request.buffetId(), period);
        return dashboardResponse(request.buffetId(), period, data);
    }

    private void requireRequest(GetKiosqueroDashboardRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }

        requireBuffetId(request.buffetId());
    }

    private void requireBuffetId(UUID buffetId) {
        if (buffetId == null) {
            throw new IllegalArgumentException("buffetId is required");
        }
    }

    private DashboardPeriod dashboardPeriod(GetKiosqueroDashboardRequest request) {
        if (isDailyDashboardRequest(request)) {
            LocalDate requestedDate = requestedDate(request.date());
            return new DashboardPeriod(
                    requestedDate,
                    requestedDate,
                    requestedDate,
                    requestedDate.minusDays(TREND_DAYS - 1L),
                    requestedDate,
                    false
            );
        }

        validateDateRange(request.from(), request.to());
        return new DashboardPeriod(
                request.to(),
                request.from(),
                request.to(),
                request.from(),
                request.to(),
                true
        );
    }

    private boolean isDailyDashboardRequest(GetKiosqueroDashboardRequest request) {
        return request.from() == null && request.to() == null;
    }

    private LocalDate requestedDate(LocalDate date) {
        return date == null ? LocalDate.now(clock) : date;
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

    private DashboardData dashboardData(UUID buffetId, DashboardPeriod period) {
        List<Purchase> purchases = purchases(buffetId, period);
        return new DashboardData(
                purchases,
                purchasesOfPeriod(purchases, period),
                inventory(buffetId),
                movements(buffetId, period)
        );
    }

    private List<Purchase> purchases(UUID buffetId, DashboardPeriod period) {
        DateTimeRange purchaseRange = range(period.trendStart(), period.trendEnd());
        return purchaseRepository.findByBuffetAndPickupOrCreationDateRange(
                buffetId,
                period.trendStart(),
                period.trendEnd(),
                purchaseRange.start(),
                purchaseRange.end()
        );
    }

    private List<Purchase> purchasesOfPeriod(
            List<Purchase> purchases,
            DashboardPeriod period
    ) {
        return purchases.stream()
                .filter(purchase -> purchase.happenedBetween(
                        period.metricStart(),
                        period.metricEnd()
                ))
                .toList();
    }

    private List<Producto> inventory(UUID buffetId) {
        return productRepository.findAllActiveWithStock(buffetId)
                .stream()
                .filter(Producto::hasInventoryData)
                .toList();
    }

    private List<MovimientoStock> movements(UUID buffetId, DashboardPeriod period) {
        DateTimeRange movementRange = range(period.metricStart(), period.metricEnd());
        return stockMovementRepository.findByBuffetAndDateRange(
                buffetId,
                movementRange.start(),
                movementRange.end()
        );
    }

    private GetKiosqueroDashboardResponse dashboardResponse(
            UUID buffetId,
            DashboardPeriod period,
            DashboardData data
    ) {
        return new GetKiosqueroDashboardResponse(
                buffetId,
                period.responseDate(),
                summary(data.metricPurchases(), data.inventory()),
                activity(data.metricPurchases()),
                products(data.metricPurchases(), data.inventory(), period.rangeMode()),
                alerts(data.metricPurchases(), data.movements()),
                trends(period.trendStart(), period.trendEnd(), data.purchases())
        );
    }

    private DateTimeRange range(LocalDate startDate, LocalDate endDate) {
        return new DateTimeRange(
                startDate.atStartOfDay(),
                endDate.plusDays(1).atStartOfDay().minusNanos(1)
        );
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
        return activityCalculator.calculate(purchases);
    }

    private DashboardProducts products(
            List<Purchase> purchases,
            List<Producto> inventory,
            boolean rangeMode
    ) {
        return productsCalculator.calculate(purchases, inventory, rangeMode);
    }

    private DashboardAlerts alerts(
            List<Purchase> purchases,
            List<MovimientoStock> movements
    ) {
        return alertsCalculator.calculate(purchases, movements);
    }

    private DashboardTrends trends(
            LocalDate startDate,
            LocalDate endDate,
            List<Purchase> purchases
    ) {
        return new DashboardTrends(trendPoints(
                startDate,
                endDate,
                purchasesByDate(purchases)
        ));
    }

    private Map<LocalDate, List<Purchase>> purchasesByDate(List<Purchase> purchases) {
        return purchases.stream()
                .filter(Purchase::hasDate)
                .collect(Collectors.groupingBy(Purchase::purchaseDate));
    }

    private List<DashboardTrendPoint> trendPoints(
            LocalDate startDate,
            LocalDate endDate,
            Map<LocalDate, List<Purchase>> purchasesByDate
    ) {
        return IntStream.range(0, daysBetweenInclusive(startDate, endDate))
                .mapToObj(index -> trendPoint(
                        startDate.plusDays(index),
                        purchasesByDate
                ))
                .toList();
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

    private int soldOutProductsCount(List<Producto> inventory) {
        return (int) inventory.stream()
                .filter(Producto::isSoldOut)
                .count();
    }

    private BigDecimal averageTicket(BigDecimal totalSold, int deliveredOrders) {
        if (deliveredOrders <= 0) {
            return BigDecimal.ZERO;
        }

        return totalSold.divide(BigDecimal.valueOf(deliveredOrders), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal totalSold(List<Purchase> purchases) {
        return purchases.stream()
                .filter(Purchase::isDelivered)
                .map(Purchase::totalAmountOrZero)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private int countStatus(List<Purchase> purchases, EstadoCompra status) {
        return (int) purchases.stream()
                .filter(purchase -> purchase.hasStatus(status))
                .count();
    }

    private int daysBetweenInclusive(LocalDate from, LocalDate to) {
        return Math.toIntExact(ChronoUnit.DAYS.between(from, to) + 1);
    }

    private record DateTimeRange(LocalDateTime start, LocalDateTime end) {
    }

    private record DashboardData(
            List<Purchase> purchases,
            List<Purchase> metricPurchases,
            List<Producto> inventory,
            List<MovimientoStock> movements
    ) {
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
