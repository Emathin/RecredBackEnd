package com.recred.backend.unit.application;

import com.recred.backend.application.GenerateDailyBuffetReport;
import com.recred.backend.application.GenerateDailyBuffetReportRequest;
import com.recred.backend.application.GenerateDailyBuffetReportResponse;
import com.recred.backend.domain.models.DailyInventorySnapshot;
import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.MedioPago;
import com.recred.backend.domain.models.MovimientoStock;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.TipoMovimientoStock;
import com.recred.backend.domain.repositories.DailyBuffetCloseRepository;
import com.recred.backend.domain.repositories.ProductRepository;
import com.recred.backend.domain.repositories.PurchaseRepository;
import com.recred.backend.domain.repositories.StockMovementRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.recred.backend.domain.models.EstadoCompra.CANCELADO;
import static com.recred.backend.domain.models.EstadoCompra.ENTREGADO;
import static com.recred.backend.domain.models.EstadoCompra.EN_PREPARACION;
import static com.recred.backend.domain.models.EstadoCompra.LISTO;
import static com.recred.backend.domain.models.EstadoCompra.PENDIENTE;
import static com.recred.backend.domain.models.EstadoCompra.RECHAZADO;
import static com.recred.backend.domain.models.EstadoCompra.VENCIDO;
import static com.recred.backend.domain.models.EstadoInventario.DISPONIBLE;
import static com.recred.backend.domain.models.EstadoInventario.SIN_STOCK;
import static com.recred.backend.domain.models.MedioPago.CREDITOS;
import static com.recred.backend.domain.models.TipoManejoInventario.CUPO_DIARIO;
import static com.recred.backend.domain.models.TipoManejoInventario.STOCK_EXACTO;
import static com.recred.backend.domain.models.TipoMovimientoStock.LIBERACION;
import static com.recred.backend.domain.models.TipoMovimientoStock.VENTA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GenerateDailyBuffetReportTest {

    private static final UUID BUFFET_ID = UUID.randomUUID();
    private static final LocalDate REPORT_DATE = LocalDate.of(2026, 5, 27);

    private final PurchaseRepository purchaseRepository = mock(PurchaseRepository.class);
    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final StockMovementRepository stockMovementRepository = mock(StockMovementRepository.class);
    private final DailyBuffetCloseRepository dailyBuffetCloseRepository = mock(DailyBuffetCloseRepository.class);

    private final GenerateDailyBuffetReport useCase = new GenerateDailyBuffetReport(
            purchaseRepository,
            productRepository,
            stockMovementRepository,
            dailyBuffetCloseRepository
    );

    @Test
    void aggregateDailySalesAndInventory() {
        Producto product = product("Alfajor", "20.00");
        Producto dailyCapacityProduct = dailyCapacityProduct("Sandwich", "50.00");
        Purchase delivered = purchase(ENTREGADO, product, 2);
        Purchase pending = purchase(PENDIENTE, product, 1);

        givenPurchasesInDateRange(List.of(delivered, pending));
        givenActiveProductsWithStock(List.of(inventoryProduct(product), dailyCapacityProduct));
        givenScheduledProductQuantity(dailyCapacityProduct.getId(), 4L);
        givenNoInventorySnapshotExists();

        GenerateDailyBuffetReportResponse report = whenGenerateReport();

        assertEquals(2, report.totalOrders());
        assertEquals(1, report.deliveredOrders());
        assertEquals(1, report.pendingOrders());
        assertEquals(new BigDecimal("40.00"), report.deliveredTotal());
        assertEquals(1, report.products().size());
        assertEquals(2, report.products().get(0).quantity());
        assertEquals(2, report.inventory().size());
        assertEquals(8, report.inventory().get(0).stockDisponible());
        assertEquals(4, report.inventory().get(0).stockMinimo());
        assertEquals(12, report.inventory().get(1).cupoMaximoDiario());
        assertEquals(8, report.inventory().get(1).cupoDisponibleDia());
    }

    @Test
    void useSnapshotInventoryWhenDailyCloseSnapshotExists() {
        DailyInventorySnapshot snapshot = new DailyInventorySnapshot(
                UUID.randomUUID(),
                "Alfajor",
                7,
                1,
                6,
                2,
                null,
                null,
                DISPONIBLE,
                STOCK_EXACTO
        );

        givenPurchasesInDateRange(List.of());
        givenMovementsInDateRange(List.of());
        givenInventorySnapshotExists(List.of(snapshot));

        GenerateDailyBuffetReportResponse report = whenGenerateReport();

        assertEquals(snapshot.productId(), report.inventory().get(0).productId());
        assertEquals(snapshot.productName(), report.inventory().get(0).productName());
        assertEquals(List.of(), report.soldOutProducts());
        verify(productRepository, never()).findAllActiveWithStock(BUFFET_ID);
    }

    @Test
    void aggregateRefundsPaymentsMovementsAndFallbackInventory() {
        Producto paidProduct = product("Alfajor", "20.00");
        Producto freeProduct = product("Producto sin precio", "1.00");
        freeProduct.setPrecio(null);
        Producto withoutInventory = product("Sin inventario", "1.00");
        Producto soldOutProduct = inventoryProduct(product("Agotado", "1.00"));
        soldOutProduct.setStockDisponible(0);
        soldOutProduct.setEstadoInventario(SIN_STOCK);
        Producto saturatedDailyCapacity = dailyCapacityProduct("Cupo lleno", "5.00");

        List<Purchase> purchases = List.of(
                purchase(ENTREGADO, paidProduct, 2, CREDITOS),
                purchase(ENTREGADO, freeProduct, 1, null),
                purchase(CANCELADO, paidProduct, 1, CREDITOS),
                purchase(RECHAZADO, paidProduct, 1, CREDITOS),
                purchase(EN_PREPARACION, paidProduct, 1, CREDITOS),
                purchase(LISTO, paidProduct, 1, CREDITOS),
                purchase(VENCIDO, paidProduct, 1, CREDITOS)
        );
        List<MovimientoStock> movements = List.of(
                movement(LIBERACION, 3),
                movement(LIBERACION, null),
                movement(VENTA, 2),
                movement(null, 9)
        );

        givenPurchasesInDateRange(purchases);
        givenMovementsInDateRange(movements);
        givenNoInventorySnapshotExists();
        givenActiveProductsWithStock(List.of(withoutInventory, soldOutProduct, saturatedDailyCapacity));
        givenScheduledProductQuantity(saturatedDailyCapacity.getId(), 20L);

        GenerateDailyBuffetReportResponse report = whenGenerateReport();

        assertEquals(7, report.totalOrders());
        assertEquals(2, report.deliveredOrders());
        assertEquals(1, report.inPreparationOrders());
        assertEquals(1, report.readyOrders());
        assertEquals(1, report.expiredOrders());
        assertEquals(1, report.cancelledOrders());
        assertEquals(1, report.rejectedOrders());
        assertEquals(new BigDecimal("40.00"), report.deliveredTotal());
        assertEquals(new BigDecimal("40.00"), report.refundedCredits());
        assertEquals(3, report.releasedReservations());
        assertEquals(2, report.products().size());
        assertEquals(1, report.salesByPaymentMethod().size());
        assertEquals(CREDITOS, report.salesByPaymentMethod().get(0).paymentMethod());
        assertEquals(2, report.stockMovements().size());
        assertEquals(List.of(soldOutProduct.getId()), report.soldOutProducts().stream()
                .map(GenerateDailyBuffetReportResponse.DailyInventorySnapshotResponse::productId)
                .toList());
        assertEquals(0, report.inventory().stream()
                .filter(item -> item.productId().equals(saturatedDailyCapacity.getId()))
                .findFirst()
                .orElseThrow()
                .cupoDisponibleDia());
    }

    private void givenPurchasesInDateRange(List<Purchase> purchases) {
        when(purchaseRepository.findByBuffetAndDateRange(eq(BUFFET_ID), any(), any()))
                .thenReturn(purchases);
    }

    private void givenMovementsInDateRange(List<MovimientoStock> movements) {
        when(stockMovementRepository.findByBuffetAndDateRange(eq(BUFFET_ID), any(), any()))
                .thenReturn(movements);
    }

    private void givenActiveProductsWithStock(List<Producto> products) {
        when(productRepository.findAllActiveWithStock(BUFFET_ID))
                .thenReturn(products);
    }

    private void givenScheduledProductQuantity(UUID productId, long quantity) {
        when(purchaseRepository.sumScheduledProductQuantity(
                eq(BUFFET_ID),
                eq(productId),
                eq(REPORT_DATE),
                anyList()
        )).thenReturn(quantity);
    }

    private void givenInventorySnapshotExists(List<DailyInventorySnapshot> snapshot) {
        when(dailyBuffetCloseRepository.findInventorySnapshotByBuffetAndDate(BUFFET_ID, REPORT_DATE))
                .thenReturn(Optional.of(snapshot));
    }

    private void givenNoInventorySnapshotExists() {
        when(dailyBuffetCloseRepository.findInventorySnapshotByBuffetAndDate(BUFFET_ID, REPORT_DATE))
                .thenReturn(Optional.empty());
    }

    private GenerateDailyBuffetReportResponse whenGenerateReport() {
        return useCase.execute(new GenerateDailyBuffetReportRequest(BUFFET_ID, REPORT_DATE));
    }

    private Purchase purchase(EstadoCompra status, Producto product, int quantity) {
        return purchase(status, product, quantity, null);
    }

    private Purchase purchase(EstadoCompra status, Producto product, int quantity, MedioPago paymentMethod) {
        Purchase purchase = new Purchase();
        purchase.setStatus(status);
        BigDecimal price = product.getPrecio() == null ? BigDecimal.ZERO : product.getPrecio();
        purchase.setTotalAmount(price.multiply(BigDecimal.valueOf(quantity)));
        purchase.setPaymentMethod(paymentMethod);
        purchase.setItems(Map.of(product, quantity));
        return purchase;
    }

    private Producto product(String name, String price) {
        Producto product = new Producto();
        product.setId(UUID.randomUUID());
        product.setNombre(name);
        product.setPrecio(new BigDecimal(price));
        return product;
    }

    private Producto inventoryProduct(Producto source) {
        Producto product = product(source.getNombre(), source.getPrecio().toPlainString());
        product.setId(source.getId());
        product.setStockActual(10);
        product.setStockReservado(2);
        product.setStockDisponible(8);
        product.setStockMinimo(4);
        product.setEstadoInventario(DISPONIBLE);
        product.setTipoManejoInventario(STOCK_EXACTO);
        return product;
    }

    private Producto dailyCapacityProduct(String name, String price) {
        Producto product = product(name, price);
        product.setStockActual(12);
        product.setStockReservado(0);
        product.setStockDisponible(12);
        product.setStockMinimo(4);
        product.setCupoMaximoDiario(12);
        product.setEstadoInventario(DISPONIBLE);
        product.setTipoManejoInventario(CUPO_DIARIO);
        return product;
    }

    private MovimientoStock movement(TipoMovimientoStock type, Integer quantity) {
        return new MovimientoStock(
                UUID.randomUUID(),
                UUID.randomUUID(),
                type,
                quantity,
                10,
                7,
                "Movimiento",
                null,
                null,
                LocalDateTime.now()
        );
    }
}
