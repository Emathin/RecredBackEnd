package com.recred.backend.application;

import com.recred.backend.domain.models.DailyBuffetReport;
import com.recred.backend.domain.models.DailyInventorySnapshot;
import com.recred.backend.domain.models.DailyMovementReport;
import com.recred.backend.domain.models.DailyPaymentMethodReport;
import com.recred.backend.domain.models.DailyProductReport;
import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.EstadoInventario;
import com.recred.backend.domain.models.MedioPago;
import com.recred.backend.domain.models.MovimientoStock;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.TipoManejoInventario;
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

    public DailyBuffetReport execute(UUID buffetId, LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay().minusNanos(1);
        List<Purchase> purchases = purchaseRepository.findByBuffetAndDateRange(
                buffetId,
                start,
                end
        );
        List<DailyInventorySnapshot> inventory = inventory(buffetId, date);
        List<MovimientoStock> movements = stockMovementRepository.findByBuffetAndDateRange(
                buffetId,
                start,
                end
        );

        return new DailyBuffetReport(
                buffetId,
                date,
                purchases.size(),
                countStatus(purchases, EstadoCompra.ENTREGADO),
                countStatus(purchases, EstadoCompra.PENDIENTE),
                countStatus(purchases, EstadoCompra.EN_PREPARACION),
                countStatus(purchases, EstadoCompra.LISTO),
                countStatus(purchases, EstadoCompra.VENCIDO),
                countStatus(purchases, EstadoCompra.CANCELADO),
                countStatus(purchases, EstadoCompra.RECHAZADO),
                deliveredTotal(purchases),
                refundedCredits(purchases),
                releasedReservations(movements),
                soldProducts(purchases),
                inventory,
                soldOutProducts(inventory),
                salesByPaymentMethod(purchases),
                stockMovements(movements)
        );
    }

    private int countStatus(List<Purchase> purchases, EstadoCompra status) {
        return (int) purchases.stream()
                .filter(purchase -> purchase.getStatus() == status)
                .count();
    }

    private BigDecimal deliveredTotal(List<Purchase> purchases) {
        return purchases.stream()
                .filter(purchase -> purchase.getStatus() == EstadoCompra.ENTREGADO)
                .map(Purchase::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<DailyProductReport> soldProducts(List<Purchase> purchases) {
        Map<UUID, ProductAccumulator> products = new LinkedHashMap<>();
        purchases.stream()
                .filter(purchase -> purchase.getStatus() == EstadoCompra.ENTREGADO)
                .forEach(purchase -> addProducts(products, purchase));

        return products.values().stream()
                .map(ProductAccumulator::toReport)
                .sorted(Comparator.comparing(DailyProductReport::productName))
                .toList();
    }

    private BigDecimal refundedCredits(List<Purchase> purchases) {
        return purchases.stream()
                .filter(this::refundedPurchase)
                .map(Purchase::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private boolean refundedPurchase(Purchase purchase) {
        return purchase.getStatus() == EstadoCompra.CANCELADO
                || purchase.getStatus() == EstadoCompra.RECHAZADO;
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

    private List<DailyPaymentMethodReport> salesByPaymentMethod(List<Purchase> purchases) {
        return purchases.stream()
                .filter(purchase -> purchase.getStatus() == EstadoCompra.ENTREGADO)
                .filter(purchase -> purchase.getPaymentMethod() != null)
                .collect(Collectors.groupingBy(Purchase::getPaymentMethod))
                .entrySet()
                .stream()
                .map(entry -> paymentMethodReport(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(report -> report.paymentMethod().name()))
                .toList();
    }

    private DailyPaymentMethodReport paymentMethodReport(
            MedioPago paymentMethod,
            List<Purchase> purchases
    ) {
        BigDecimal total = purchases.stream()
                .map(Purchase::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new DailyPaymentMethodReport(paymentMethod, purchases.size(), total);
    }

    private List<DailyMovementReport> stockMovements(List<MovimientoStock> movements) {
        return movements.stream()
                .filter(movement -> movement.tipo() != null)
                .collect(Collectors.groupingBy(MovimientoStock::tipo))
                .entrySet()
                .stream()
                .map(entry -> movementReport(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(report -> report.movementType().name()))
                .toList();
    }

    private DailyMovementReport movementReport(
            TipoMovimientoStock movementType,
            List<MovimientoStock> movements
    ) {
        int quantity = movements.stream()
                .map(MovimientoStock::cantidad)
                .filter(value -> value != null)
                .reduce(0, Integer::sum);
        return new DailyMovementReport(movementType, quantity);
    }

    private void addProducts(Map<UUID, ProductAccumulator> products, Purchase purchase) {
        purchase.getItems().forEach((product, quantity) -> products
                .computeIfAbsent(product.getId(), id -> new ProductAccumulator(product))
                .add(quantity));
    }

    private List<DailyInventorySnapshot> inventory(UUID buffetId, LocalDate date) {
        if (dailyBuffetCloseRepository != null) {
            List<DailyInventorySnapshot> snapshot = dailyBuffetCloseRepository
                    .findInventorySnapshotByBuffetAndDate(buffetId, date)
                    .orElse(List.of());
            if (!snapshot.isEmpty()) {
                return snapshot;
            }
        }

        return productRepository.findAllActiveWithStock(buffetId).stream()
                .filter(this::hasInventory)
                .map(product -> inventorySnapshot(buffetId, date, product))
                .sorted(Comparator.comparing(DailyInventorySnapshot::productName))
                .toList();
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
        return hasDailyCapacity(product)
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
        return product.getStockActual() != null
                || product.getStockReservado() != null
                || product.getStockMinimo() != null
                || product.getCupoMaximoDiario() != null
                || product.getEstadoInventario() != null
                || product.getTipoManejoInventario() != null;
    }

    private boolean hasDailyCapacity(Producto product) {
        return product.getTipoManejoInventario() == TipoManejoInventario.CUPO_DIARIO
                && product.getCupoMaximoDiario() != null
                && product.getCupoMaximoDiario() > 0;
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

        DailyProductReport toReport() {
            BigDecimal price = product.getPrecio() == null
                    ? BigDecimal.ZERO
                    : product.getPrecio();
            return new DailyProductReport(
                    product.getId(),
                    product.getNombre(),
                    quantity,
                    price.multiply(BigDecimal.valueOf(quantity))
            );
        }
    }
}
