package com.recred.backend.unit.application;

import com.recred.backend.application.GenerateDailyBuffetReport;
import com.recred.backend.domain.models.DailyBuffetReport;
import com.recred.backend.domain.models.DailyInventorySnapshot;
import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.EstadoInventario;
import com.recred.backend.domain.models.MedioPago;
import com.recred.backend.domain.models.MovimientoStock;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.TipoManejoInventario;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GenerateDailyBuffetReportTest {

    @Test
    void execute_ShouldAggregateDailySalesAndInventory() {
        PurchaseRepository purchaseRepository = mock(PurchaseRepository.class);
        ProductRepository productRepository = mock(ProductRepository.class);
        StockMovementRepository stockMovementRepository = mock(StockMovementRepository.class);
        GenerateDailyBuffetReport useCase = new GenerateDailyBuffetReport(
                purchaseRepository,
                productRepository,
                stockMovementRepository
        );
        UUID buffetId = UUID.randomUUID();
        Producto product = product("Alfajor", "20.00");
        Producto dailyCapacityProduct = dailyCapacityProduct("Sandwich", "50.00");
        Purchase delivered = purchase(EstadoCompra.ENTREGADO, product, 2);
        Purchase pending = purchase(EstadoCompra.PENDIENTE, product, 1);
        LocalDate date = LocalDate.of(2026, 5, 27);

        when(purchaseRepository.findByBuffetAndDateRange(eq(buffetId), any(), any()))
                .thenReturn(List.of(delivered, pending));
        when(productRepository.findAllActiveWithStock(buffetId))
                .thenReturn(List.of(inventoryProduct(product), dailyCapacityProduct));
        when(purchaseRepository.sumScheduledProductQuantity(
                eq(buffetId),
                eq(dailyCapacityProduct.getId()),
                eq(date),
                anyList()
        )).thenReturn(4L);

        DailyBuffetReport report = useCase.execute(buffetId, date);

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
    void execute_WhenDailyCloseSnapshotExists_ShouldUseSnapshotInventory() {
        PurchaseRepository purchaseRepository = mock(PurchaseRepository.class);
        ProductRepository productRepository = mock(ProductRepository.class);
        StockMovementRepository stockMovementRepository = mock(StockMovementRepository.class);
        DailyBuffetCloseRepository dailyBuffetCloseRepository =
                mock(DailyBuffetCloseRepository.class);
        GenerateDailyBuffetReport useCase = new GenerateDailyBuffetReport(
                purchaseRepository,
                productRepository,
                stockMovementRepository,
                dailyBuffetCloseRepository
        );
        UUID buffetId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 5, 27);
        DailyInventorySnapshot snapshot = new DailyInventorySnapshot(
                UUID.randomUUID(),
                "Alfajor",
                7,
                1,
                6,
                2,
                null,
                null,
                EstadoInventario.DISPONIBLE,
                TipoManejoInventario.STOCK_EXACTO
        );

        when(purchaseRepository.findByBuffetAndDateRange(eq(buffetId), any(), any()))
                .thenReturn(List.of());
        when(stockMovementRepository.findByBuffetAndDateRange(eq(buffetId), any(), any()))
                .thenReturn(List.of());
        when(dailyBuffetCloseRepository.findInventorySnapshotByBuffetAndDate(buffetId, date))
                .thenReturn(Optional.of(List.of(snapshot)));

        DailyBuffetReport report = useCase.execute(buffetId, date);

        assertEquals(List.of(snapshot), report.inventory());
        assertEquals(List.of(), report.soldOutProducts());
        verify(productRepository, never()).findAllActiveWithStock(buffetId);
    }

    @Test
    void execute_ShouldAggregateRefundsPaymentsMovementsAndFallbackInventory() {
        PurchaseRepository purchaseRepository = mock(PurchaseRepository.class);
        ProductRepository productRepository = mock(ProductRepository.class);
        StockMovementRepository stockMovementRepository = mock(StockMovementRepository.class);
        DailyBuffetCloseRepository dailyBuffetCloseRepository =
                mock(DailyBuffetCloseRepository.class);
        GenerateDailyBuffetReport useCase = new GenerateDailyBuffetReport(
                purchaseRepository,
                productRepository,
                stockMovementRepository,
                dailyBuffetCloseRepository
        );
        UUID buffetId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 5, 27);
        Producto paidProduct = product("Alfajor", "20.00");
        Producto freeProduct = product("Producto sin precio", "1.00");
        freeProduct.setPrecio(null);
        Producto withoutInventory = product("Sin inventario", "1.00");
        Producto soldOutProduct = inventoryProduct(product("Agotado", "1.00"));
        soldOutProduct.setStockDisponible(0);
        soldOutProduct.setEstadoInventario(EstadoInventario.SIN_STOCK);
        Producto saturatedDailyCapacity = dailyCapacityProduct("Cupo lleno", "5.00");
        List<Purchase> purchases = List.of(
                purchase(EstadoCompra.ENTREGADO, paidProduct, 2, MedioPago.CREDITOS),
                purchase(EstadoCompra.ENTREGADO, freeProduct, 1, null),
                purchase(EstadoCompra.CANCELADO, paidProduct, 1, MedioPago.CREDITOS),
                purchase(EstadoCompra.RECHAZADO, paidProduct, 1, MedioPago.CREDITOS),
                purchase(EstadoCompra.EN_PREPARACION, paidProduct, 1, MedioPago.CREDITOS),
                purchase(EstadoCompra.LISTO, paidProduct, 1, MedioPago.CREDITOS),
                purchase(EstadoCompra.VENCIDO, paidProduct, 1, MedioPago.CREDITOS)
        );
        List<MovimientoStock> movements = List.of(
                movement(TipoMovimientoStock.LIBERACION, 3),
                movement(TipoMovimientoStock.LIBERACION, null),
                movement(TipoMovimientoStock.VENTA, 2),
                movement(null, 9)
        );
        when(purchaseRepository.findByBuffetAndDateRange(eq(buffetId), any(), any()))
                .thenReturn(purchases);
        when(stockMovementRepository.findByBuffetAndDateRange(eq(buffetId), any(), any()))
                .thenReturn(movements);
        when(dailyBuffetCloseRepository.findInventorySnapshotByBuffetAndDate(buffetId, date))
                .thenReturn(Optional.empty());
        when(productRepository.findAllActiveWithStock(buffetId)).thenReturn(List.of(
                withoutInventory,
                soldOutProduct,
                saturatedDailyCapacity
        ));
        when(purchaseRepository.sumScheduledProductQuantity(
                eq(buffetId),
                eq(saturatedDailyCapacity.getId()),
                eq(date),
                anyList()
        )).thenReturn(20L);

        DailyBuffetReport report = useCase.execute(buffetId, date);

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
        assertEquals(MedioPago.CREDITOS,
                report.salesByPaymentMethod().get(0).paymentMethod());
        assertEquals(2, report.stockMovements().size());
        assertEquals(List.of(soldOutProduct.getId()), report.soldOutProducts().stream()
                .map(DailyInventorySnapshot::productId)
                .toList());
        assertEquals(0, report.inventory().stream()
                .filter(item -> item.productId().equals(saturatedDailyCapacity.getId()))
                .findFirst()
                .orElseThrow()
                .cupoDisponibleDia());
    }

    private Purchase purchase(EstadoCompra status, Producto product, int quantity) {
        return purchase(status, product, quantity, null);
    }

    private Purchase purchase(
            EstadoCompra status,
            Producto product,
            int quantity,
            MedioPago paymentMethod
    ) {
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
        product.setEstadoInventario(EstadoInventario.DISPONIBLE);
        product.setTipoManejoInventario(TipoManejoInventario.STOCK_EXACTO);
        return product;
    }

    private Producto dailyCapacityProduct(String name, String price) {
        Producto product = product(name, price);
        product.setStockActual(12);
        product.setStockReservado(0);
        product.setStockDisponible(12);
        product.setStockMinimo(4);
        product.setCupoMaximoDiario(12);
        product.setEstadoInventario(EstadoInventario.DISPONIBLE);
        product.setTipoManejoInventario(TipoManejoInventario.CUPO_DIARIO);
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
