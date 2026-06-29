package com.recred.backend.application;

import com.recred.backend.application.GenerateDailyBuffetReportResponse.DailyInventorySnapshotResponse;
import com.recred.backend.application.GenerateDailyBuffetReportResponse.DailyMovementReportResponse;
import com.recred.backend.application.GenerateDailyBuffetReportResponse.DailyPaymentMethodReportResponse;
import com.recred.backend.application.GenerateDailyBuffetReportResponse.DailyProductReportResponse;
import com.recred.backend.domain.models.DailyInventorySnapshot;
import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.EstadoInventario;
import com.recred.backend.domain.models.MedioPago;
import com.recred.backend.domain.models.MovimientoStock;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.TipoMovimientoStock;
import com.recred.backend.domain.repositories.ProductRepository;
import com.recred.backend.domain.repositories.PurchaseRepository;
import com.recred.backend.domain.repositories.DailyBuffetCloseRepository;
import com.recred.backend.domain.repositories.StockMovementRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class GenerateDailyBuffetReport {

    private static final List<EstadoCompra> ACTIVE_ADVANCE_STATUSES = List.of(
            EstadoCompra.PENDIENTE,
            EstadoCompra.EN_PREPARACION,
            EstadoCompra.LISTO
    );

    private final PurchaseRepository purchaseRepository;
    private final ProductRepository productRepository;
    private final StockMovementRepository stockMovementRepository;
    private final DailyBuffetCloseRepository dailyBuffetCloseRepository;

    public GenerateDailyBuffetReport(
            PurchaseRepository purchaseRepository,
            ProductRepository productRepository,
            StockMovementRepository stockMovementRepository
    ) {
        this(purchaseRepository, productRepository, stockMovementRepository, null);
    }

    public GenerateDailyBuffetReport(
            PurchaseRepository purchaseRepository,
            ProductRepository productRepository,
            StockMovementRepository stockMovementRepository,
            DailyBuffetCloseRepository dailyBuffetCloseRepository
    ) {
        this.purchaseRepository = purchaseRepository;
        this.productRepository = productRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.dailyBuffetCloseRepository = dailyBuffetCloseRepository;
    }

    public GenerateDailyBuffetReportResponse execute(GenerateDailyBuffetReportRequest request) {
        validate(request);
        ReportSource source = reportSource(request);
        return response(request, source);
    }

    private void validate(GenerateDailyBuffetReportRequest request) {
        required(request, "request is required");
        required(request.buffetId(), "buffetId is required");
        required(request.date(), "date is required");
    }

    private ReportSource reportSource(GenerateDailyBuffetReportRequest request) {
        LocalDateTime start = request.date().atStartOfDay();
        LocalDateTime end = request.date().plusDays(1).atStartOfDay().minusNanos(1);
        return new ReportSource(
                purchaseRepository.findByBuffetAndDateRange(request.buffetId(), start, end),
                inventory(request.buffetId(), request.date()),
                stockMovementRepository.findByBuffetAndDateRange(request.buffetId(), start, end)
        );
    }

    private GenerateDailyBuffetReportResponse response(
            GenerateDailyBuffetReportRequest request,
            ReportSource source
    ) {
        return new GenerateDailyBuffetReportResponse(
                request.buffetId(),
                request.date(),
                source.purchases().size(),
                countStatus(source.purchases(), EstadoCompra.ENTREGADO),
                countStatus(source.purchases(), EstadoCompra.PENDIENTE),
                countStatus(source.purchases(), EstadoCompra.EN_PREPARACION),
                countStatus(source.purchases(), EstadoCompra.LISTO),
                countStatus(source.purchases(), EstadoCompra.VENCIDO),
                countStatus(source.purchases(), EstadoCompra.CANCELADO),
                countStatus(source.purchases(), EstadoCompra.RECHAZADO),
                deliveredTotal(source.purchases()),
                refundedCredits(source.purchases()),
                releasedReservations(source.movements()),
                soldProducts(source.purchases()),
                inventoryResponse(source.inventory()),
                inventoryResponse(soldOutProducts(source.inventory())),
                salesByPaymentMethod(source.purchases()),
                stockMovements(source.movements())
        );
    }

    private int countStatus(List<Purchase> purchases, EstadoCompra status) {
        return (int) purchases.stream()
                .filter(purchase -> purchase.hasStatus(status))
                .count();
    }

    private BigDecimal deliveredTotal(List<Purchase> purchases) {
        return purchases.stream()
                .filter(Purchase::isDelivered)
                .map(Purchase::totalAmountOrZero)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<DailyProductReportResponse> soldProducts(List<Purchase> purchases) {
        Map<UUID, ProductAccumulator> products = new LinkedHashMap<>();
        purchases.stream()
                .filter(Purchase::isDelivered)
                .forEach(purchase -> addProducts(products, purchase));

        return products.values().stream()
                .map(ProductAccumulator::toReport)
                .sorted(Comparator.comparing(DailyProductReportResponse::productName))
                .toList();
    }

    private BigDecimal refundedCredits(List<Purchase> purchases) {
        return purchases.stream()
                .filter(Purchase::refundsCredits)
                .map(Purchase::totalAmountOrZero)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private int releasedReservations(List<MovimientoStock> movements) {
        return movements.stream()
                .filter(movement -> movement.tipo() == TipoMovimientoStock.LIBERACION)
                .map(MovimientoStock::cantidad)
                .filter(quantity -> quantity != null)
                .reduce(0, Integer::sum);
    }

    private List<DailyInventorySnapshot> soldOutProducts(
            List<DailyInventorySnapshot> inventory
    ) {
        return inventory.stream()
                .filter(item -> item.estadoInventario() == EstadoInventario.SIN_STOCK)
                .toList();
    }

    private List<DailyInventorySnapshotResponse> inventoryResponse(
            List<DailyInventorySnapshot> inventory
    ) {
        return inventory.stream()
                .map(DailyInventorySnapshotResponse::fromDomain)
                .toList();
    }

    private List<DailyPaymentMethodReportResponse> salesByPaymentMethod(
            List<Purchase> purchases
    ) {
        return purchases.stream()
                .filter(Purchase::isDelivered)
                .filter(purchase -> purchase.getPaymentMethod() != null)
                .collect(Collectors.groupingBy(Purchase::getPaymentMethod))
                .entrySet()
                .stream()
                .map(entry -> paymentMethodReport(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(report -> report.paymentMethod().name()))
                .toList();
    }

    private DailyPaymentMethodReportResponse paymentMethodReport(
            MedioPago paymentMethod,
            List<Purchase> purchases
    ) {
        BigDecimal total = purchases.stream()
                .map(Purchase::totalAmountOrZero)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new DailyPaymentMethodReportResponse(paymentMethod, purchases.size(), total);
    }

    private List<DailyMovementReportResponse> stockMovements(List<MovimientoStock> movements) {
        return movements.stream()
                .filter(movement -> movement.tipo() != null)
                .collect(Collectors.groupingBy(MovimientoStock::tipo))
                .entrySet()
                .stream()
                .map(entry -> movementReport(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(report -> report.movementType().name()))
                .toList();
    }

    private DailyMovementReportResponse movementReport(
            TipoMovimientoStock movementType,
            List<MovimientoStock> movements
    ) {
        int quantity = movements.stream()
                .map(MovimientoStock::cantidad)
                .filter(value -> value != null)
                .reduce(0, Integer::sum);
        return new DailyMovementReportResponse(movementType, quantity);
    }

    private void addProducts(Map<UUID, ProductAccumulator> products, Purchase purchase) {
        purchase.getItems().forEach((product, quantity) -> products
                .computeIfAbsent(product.getId(), id -> new ProductAccumulator(product))
                .add(quantity));
    }

    private List<DailyInventorySnapshot> inventory(UUID buffetId, LocalDate date) {
        if (dailyBuffetCloseRepository != null) {
            List<DailyInventorySnapshot> snapshot = savedInventorySnapshot(buffetId, date);
            if (hasSavedInventorySnapshot(snapshot)) {
                return snapshot;
            }
        }

        return productRepository.findAllActiveWithStock(buffetId).stream()
                .filter(this::hasInventory)
                .map(product -> inventorySnapshot(buffetId, date, product))
                .sorted(Comparator.comparing(DailyInventorySnapshot::productName))
                .toList();
    }

    private List<DailyInventorySnapshot> savedInventorySnapshot(
            UUID buffetId,
            LocalDate date
    ) {
        return dailyBuffetCloseRepository
                .findInventorySnapshotByBuffetAndDate(buffetId, date)
                .orElse(List.of());
    }

    private boolean hasSavedInventorySnapshot(List<DailyInventorySnapshot> snapshot) {
        return !snapshot.isEmpty();
    }

    private DailyInventorySnapshot inventorySnapshot(
            UUID buffetId,
            LocalDate date,
            Producto product
    ) {
        return new DailyInventorySnapshot(
                product.getId(),
                product.getNombre(),
                product.getStockActual(),
                product.getStockReservado(),
                product.getStockDisponible(),
                product.getStockMinimo(),
                product.getCupoMaximoDiario(),
                cupoDisponibleDia(buffetId, date, product),
                product.getEstadoInventario(),
                product.getTipoManejoInventario()
        );
    }

    private Integer cupoDisponibleDia(UUID buffetId, LocalDate date, Producto product) {
        return product.hasDailyCapacityLimit()
                ? dailyCapacityAvailable(buffetId, date, product)
                : product.getCupoDisponibleDia();
    }

    private Integer dailyCapacityAvailable(UUID buffetId, LocalDate date, Producto product) {
        long reserved = purchaseRepository.sumScheduledProductQuantity(
                buffetId,
                product.getId(),
                date,
                ACTIVE_ADVANCE_STATUSES
        );
        return Math.max(0, product.getCupoMaximoDiario() - (int) reserved);
    }

    private boolean hasInventory(Producto product) {
        return product.hasInventoryData();
    }

    private <T> T required(T value, String message) {
        return Optional.ofNullable(value)
                .orElseThrow(() -> new IllegalArgumentException(message));
    }

    private static class ProductAccumulator {
        private final Producto product;
        private int quantity;

        ProductAccumulator(Producto product) {
            this.product = product;
        }

        void add(int quantityToAdd) {
            quantity += quantityToAdd;
        }

        DailyProductReportResponse toReport() {
            BigDecimal price = product.priceOrZero();
            return new DailyProductReportResponse(
                    product.getId(),
                    product.getNombre(),
                    quantity,
                    price.multiply(BigDecimal.valueOf(quantity))
            );
        }
    }

    private record ReportSource(
            List<Purchase> purchases,
            List<DailyInventorySnapshot> inventory,
            List<MovimientoStock> movements
    ) {
    }
}
