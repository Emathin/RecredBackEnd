package com.recred.backend.unit.application;

import com.recred.backend.application.GetKiosqueroDashboard;
import com.recred.backend.domain.models.CategoriaProducto;
import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.EstadoInventario;
import com.recred.backend.domain.models.KiosqueroDashboard;
import com.recred.backend.domain.models.MovimientoStock;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.TipoCompra;
import com.recred.backend.domain.models.TipoManejoInventario;
import com.recred.backend.domain.models.TipoMovimientoStock;
import com.recred.backend.domain.repositories.ProductRepository;
import com.recred.backend.domain.repositories.PurchaseRepository;
import com.recred.backend.domain.repositories.StockMovementRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GetKiosqueroDashboardTest {

    @Test
    void execute_ShouldBuildOperationalDashboardWithoutFullStockList() {
        UUID buffetId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 6, 10);
        Producto alfajor = product("Alfajor", "40.00", "Golosinas");
        Producto sandwich = product("Sandwich", "50.00", "Comida");
        Producto fruta = product("Fruta", "20.00", "Frutas");
        Producto agua = product("Agua", "15.00", "Bebidas");
        List<Purchase> purchases = List.of(
                purchase(EstadoCompra.ENTREGADO, TipoCompra.PRESENCIAL, alfajor,
                        2, "80.00", date.atTime(9, 30), null),
                purchase(EstadoCompra.ENTREGADO, TipoCompra.ANTICIPADA, sandwich,
                        1, "50.00", date.atTime(10, 0), "Primer recreo"),
                purchase(EstadoCompra.PENDIENTE, TipoCompra.ANTICIPADA, fruta,
                        1, "20.00", date.atTime(10, 10), "Segundo recreo"),
                purchase(EstadoCompra.LISTO, TipoCompra.ANTICIPADA, fruta,
                        1, "20.00", date.atTime(10, 20), "Segundo recreo"),
                purchase(EstadoCompra.VENCIDO, TipoCompra.ANTICIPADA, agua,
                        1, "15.00", date.atTime(10, 30), "Primer recreo"),
                purchase(EstadoCompra.RECHAZADO, TipoCompra.ANTICIPADA, alfajor,
                        1, "40.00", date.atTime(11, 0), "Primer recreo"),
                purchase(EstadoCompra.ENTREGADO, TipoCompra.PRESENCIAL, alfajor,
                        1, "40.00", date.minusDays(1).atTime(9, 30), null)
        );
        List<Producto> inventory = List.of(
                inventoryProduct(alfajor, 10, 6, 4, 3, EstadoInventario.DISPONIBLE),
                inventoryProduct(fruta, 5, 0, 2, 4, EstadoInventario.BAJO_STOCK),
                inventoryProduct(agua, 0, 0, 0, 4, EstadoInventario.SIN_STOCK)
        );
        List<MovimientoStock> movements = List.of(
                movement(TipoMovimientoStock.LIBERACION, 3, 1),
                movement(TipoMovimientoStock.VENTA, 2, 0)
        );
        PurchaseRepository purchaseRepository = mock(PurchaseRepository.class);
        ProductRepository productRepository = mock(ProductRepository.class);
        StockMovementRepository stockMovementRepository = mock(StockMovementRepository.class);
        GetKiosqueroDashboard useCase = new GetKiosqueroDashboard(
                purchaseRepository,
                productRepository,
                stockMovementRepository,
                Clock.fixed(
                        Instant.parse("2026-06-10T15:00:00Z"),
                        ZoneId.of("America/Argentina/Buenos_Aires")
                )
        );

        when(purchaseRepository.findByBuffetAndDateRange(eq(buffetId), any(), any()))
                .thenReturn(purchases);
        when(productRepository.findAllActiveWithStock(buffetId)).thenReturn(inventory);
        when(stockMovementRepository.findByBuffetAndDateRange(eq(buffetId), any(), any()))
                .thenReturn(movements);

        KiosqueroDashboard dashboard = useCase.execute(buffetId, date);

        assertEquals(new BigDecimal("130.00"), dashboard.summary().totalSold());
        assertEquals(6, dashboard.summary().totalOrders());
        assertEquals(2, dashboard.summary().deliveredOrders());
        assertEquals(new BigDecimal("65.00"), dashboard.summary().averageTicket());
        assertEquals(1, dashboard.summary().pendingOrders());
        assertEquals(1, dashboard.summary().soldOutProducts());
        assertEquals(2, dashboard.activity().salesByTimeSlot().size());
        assertEquals("Primer recreo",
                dashboard.activity().salesByTimeSlot().get(0).timeSlot());
        assertEquals(LocalTime.of(9, 30),
                dashboard.activity().salesByTimeSlot().get(0).pickupSlotStartTime());
        assertEquals(LocalTime.of(9, 45),
                dashboard.activity().salesByTimeSlot().get(0).pickupSlotEndTime());
        assertEquals("Compra espontanea",
                dashboard.activity().salesByTimeSlot().get(1).timeSlot());
        assertEquals(2, dashboard.activity().salesByCategory().size());
        assertEquals("Golosinas",
                dashboard.activity().salesByCategory().get(0).categoryName());
        assertEquals(2, dashboard.activity().salesByCategory().get(0).quantity());
        assertEquals(new BigDecimal("80.00"),
                dashboard.activity().salesByCategory().get(0).total());
        assertEquals(5, dashboard.activity().ordersByPurchaseType().get(1).orders());
        assertEquals("Alfajor", dashboard.products().topSoldProducts().get(0).productName());
        assertEquals("Alfajor", dashboard.products().mostReservedProducts().get(0).productName());
        assertFalse(dashboard.products().productsNeedingRestock().isEmpty());
        assertEquals(1, dashboard.products().soldOutProducts().size());
        assertEquals(1, dashboard.alerts().expiredOrders());
        assertEquals(3, dashboard.alerts().releasedReservations());
        assertEquals(new BigDecimal("40.00"), dashboard.alerts().refundedCredits());
        assertEquals(1, dashboard.alerts().soldOutEvents());
        assertFalse(dashboard.alerts().items().isEmpty());
        assertEquals(7, dashboard.trends().lastSevenDays().size());
        assertEquals(7, dashboard.trends().salesByDay().size());
        assertEquals(date, dashboard.trends().lastSevenDays().get(6).date());
        assertEquals(6, dashboard.trends().lastSevenDays().get(6).createdOrders());
        assertEquals(1, dashboard.trends().lastSevenDays().get(6).pendingOrders());
        assertEquals(1, dashboard.trends().lastSevenDays().get(6).readyOrders());
        assertEquals(1, dashboard.trends().lastSevenDays().get(6).rejectedOrders());
        assertEquals(1, dashboard.trends().lastSevenDays().get(6).expiredOrders());
    }

    @Test
    void execute_ShouldBuildDashboardForRequestedPeriod() {
        UUID buffetId = UUID.randomUUID();
        LocalDate from = LocalDate.of(2026, 6, 1);
        LocalDate to = LocalDate.of(2026, 6, 14);
        Producto alfajor = product("Alfajor", "40.00", "Golosinas");
        List<Purchase> purchases = List.of(
                purchase(EstadoCompra.ENTREGADO, TipoCompra.PRESENCIAL, alfajor,
                        1, "40.00", from.atTime(9, 0), null),
                purchase(EstadoCompra.RECHAZADO, TipoCompra.ANTICIPADA, alfajor,
                        1, "40.00", from.plusDays(1).atTime(10, 0), "Primer recreo"),
                purchase(EstadoCompra.PENDIENTE, TipoCompra.ANTICIPADA, alfajor,
                        1, "40.00", from.plusDays(5).atTime(10, 0), "Primer recreo"),
                purchase(EstadoCompra.CANCELADO, TipoCompra.ANTICIPADA, alfajor,
                        1, "40.00", to.atTime(10, 0), "Primer recreo"),
                purchase(EstadoCompra.ENTREGADO, TipoCompra.PRESENCIAL, alfajor,
                        1, "40.00", from.minusDays(1).atTime(9, 0), null)
        );
        PurchaseRepository purchaseRepository = mock(PurchaseRepository.class);
        ProductRepository productRepository = mock(ProductRepository.class);
        StockMovementRepository stockMovementRepository = mock(StockMovementRepository.class);
        GetKiosqueroDashboard useCase = new GetKiosqueroDashboard(
                purchaseRepository,
                productRepository,
                stockMovementRepository,
                Clock.fixed(
                        Instant.parse("2026-06-10T15:00:00Z"),
                        ZoneId.of("America/Argentina/Buenos_Aires")
                )
        );

        when(purchaseRepository.findByBuffetAndDateRange(eq(buffetId), any(), any()))
                .thenReturn(purchases);
        when(productRepository.findAllActiveWithStock(buffetId)).thenReturn(List.of());
        when(stockMovementRepository.findByBuffetAndDateRange(eq(buffetId), any(), any()))
                .thenReturn(List.of());

        KiosqueroDashboard dashboard = useCase.execute(buffetId, null, from, to);

        assertEquals(to, dashboard.date());
        assertEquals(4, dashboard.summary().totalOrders());
        assertEquals(1, dashboard.summary().deliveredOrders());
        assertEquals(new BigDecimal("40.00"), dashboard.summary().totalSold());
        assertEquals(14, dashboard.trends().lastSevenDays().size());
        assertEquals(14, dashboard.trends().salesByDay().size());
        assertEquals("Alfajor", dashboard.products().mostReservedProducts().get(0).productName());
        assertEquals(3, dashboard.products().mostReservedProducts().get(0).quantity());
        assertEquals(1, dashboard.trends().salesByDay().get(0).deliveredOrders());
        assertEquals(1, dashboard.trends().salesByDay().get(1).rejectedOrders());
        assertEquals(1, dashboard.trends().salesByDay().get(5).pendingOrders());
        assertEquals(1, dashboard.trends().salesByDay().get(13).cancelledOrders());
        verify(purchaseRepository).findByBuffetAndDateRange(
                buffetId,
                from.atStartOfDay(),
                to.plusDays(1).atStartOfDay().minusNanos(1)
        );
        verify(stockMovementRepository).findByBuffetAndDateRange(
                buffetId,
                from.atStartOfDay(),
                to.plusDays(1).atStartOfDay().minusNanos(1)
        );
    }

    @Test
    void execute_ShouldRejectInvalidInputsAndRanges() {
        GetKiosqueroDashboard useCase = useCase(
                mock(PurchaseRepository.class),
                mock(ProductRepository.class),
                mock(StockMovementRepository.class)
        );
        UUID buffetId = UUID.randomUUID();

        assertThrows(IllegalArgumentException.class,
                () -> useCase.execute(null, LocalDate.of(2026, 6, 10)));
        assertThrows(IllegalArgumentException.class,
                () -> useCase.execute(buffetId, null, LocalDate.of(2026, 6, 1), null));
        assertThrows(IllegalArgumentException.class,
                () -> useCase.execute(
                        buffetId,
                        null,
                        LocalDate.of(2026, 6, 2),
                        LocalDate.of(2026, 6, 1)
                ));
        assertThrows(IllegalArgumentException.class,
                () -> useCase.execute(
                        buffetId,
                        null,
                        LocalDate.of(2025, 1, 1),
                        LocalDate.of(2026, 6, 1)
                ));
    }

    @Test
    void execute_ShouldHandleNullDatesAndSparseDashboardData() {
        UUID buffetId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 6, 10);
        Producto nullPriceProduct = product("Producto sin precio", "1.00");
        nullPriceProduct.setPrecio(null);
        Producto uncategorized = product("Sin categoria real", "10.00");
        Producto unnamedCategory = product("Categoria sin nombre", "1.00");
        CategoriaProducto category = category(null);
        unnamedCategory.setCategoria(category);
        Producto invalidProduct = product("Cantidad invalida", "2.00");
        Producto withoutInventory = product("Sin inventario", "1.00");
        Producto reservedWithoutQuantity = product("Reserva nula", "1.00");
        reservedWithoutQuantity.setStockActual(5);
        reservedWithoutQuantity.setStockReservado(null);
        Producto lowWithoutState = product("Bajo sin estado", "1.00");
        lowWithoutState.setStockActual(2);
        lowWithoutState.setStockDisponible(1);
        lowWithoutState.setStockMinimo(2);
        Producto soldOutByQuantity = product("Agotado por cantidad", "1.00");
        soldOutByQuantity.setStockActual(2);
        soldOutByQuantity.setStockDisponible(0);
        soldOutByQuantity.setStockMinimo(1);
        soldOutByQuantity.setEstadoInventario(EstadoInventario.DISPONIBLE);
        List<Purchase> purchases = List.of(
                purchaseWithItems(
                        EstadoCompra.ENTREGADO,
                        TipoCompra.PRESENCIAL,
                        Map.of(nullPriceProduct, 1),
                        null,
                        date.atTime(8, 30),
                        null
                ),
                purchaseWithItems(
                        EstadoCompra.ENTREGADO,
                        TipoCompra.PRESENCIAL,
                        Map.of(uncategorized, 1),
                        "10.00",
                        date.atTime(9, 0),
                        null
                ),
                purchaseWithItems(
                        EstadoCompra.ENTREGADO,
                        TipoCompra.PRESENCIAL,
                        Map.of(unnamedCategory, 1),
                        "1.00",
                        date.atTime(9, 15),
                        null
                ),
                purchaseWithItems(
                        EstadoCompra.ENTREGADO,
                        TipoCompra.PRESENCIAL,
                        invalidItems(invalidProduct),
                        "0.00",
                        date.atTime(9, 30),
                        null
                ),
                purchaseWithItems(
                        EstadoCompra.PENDIENTE,
                        TipoCompra.ANTICIPADA,
                        Map.of(uncategorized, 1),
                        "10.00",
                        null,
                        "Primer recreo"
                )
        );
        PurchaseRepository purchaseRepository = mock(PurchaseRepository.class);
        ProductRepository productRepository = mock(ProductRepository.class);
        StockMovementRepository stockMovementRepository = mock(StockMovementRepository.class);
        GetKiosqueroDashboard useCase = useCase(
                purchaseRepository,
                productRepository,
                stockMovementRepository
        );

        when(purchaseRepository.findByBuffetAndDateRange(eq(buffetId), any(), any()))
                .thenReturn(purchases);
        when(productRepository.findAllActiveWithStock(buffetId)).thenReturn(List.of(
                withoutInventory,
                reservedWithoutQuantity,
                lowWithoutState,
                soldOutByQuantity
        ));
        when(stockMovementRepository.findByBuffetAndDateRange(eq(buffetId), any(), any()))
                .thenReturn(List.of(
                        movement(TipoMovimientoStock.LIBERACION, 3, null),
                        movement(TipoMovimientoStock.AJUSTE, 1, 0)
                ));

        KiosqueroDashboard dashboard = useCase.execute(buffetId, null);

        assertEquals(date, dashboard.date());
        assertEquals(new BigDecimal("11.00"), dashboard.summary().totalSold());
        assertTrue(dashboard.activity().salesByCategory().stream()
                .anyMatch(categorySales -> "Sin categoria".equals(categorySales.categoryName())));
        assertTrue(dashboard.products().topSoldProducts().stream()
                .anyMatch(metric -> "Producto sin precio".equals(metric.productName())
                        && BigDecimal.ZERO.equals(metric.total())));
        assertTrue(dashboard.products().productsNeedingRestock().stream()
                .anyMatch(item -> "Bajo sin estado".equals(item.productName())
                        && item.estadoInventario() == null));
        assertTrue(dashboard.products().soldOutProducts().stream()
                .anyMatch(item -> "Agotado por cantidad".equals(item.productName())));
        assertEquals(1, dashboard.alerts().soldOutEvents());
    }

    @Test
    void execute_ShouldUseZeroAverageWhenThereAreNoDeliveredOrders() {
        UUID buffetId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 6, 10);
        Producto product = product("Pendiente", "10.00");
        Purchase pending = purchase(
                EstadoCompra.PENDIENTE,
                TipoCompra.ANTICIPADA,
                product,
                1,
                "10.00",
                date.atTime(10, 0),
                "Primer recreo"
        );
        PurchaseRepository purchaseRepository = mock(PurchaseRepository.class);
        ProductRepository productRepository = mock(ProductRepository.class);
        StockMovementRepository stockMovementRepository = mock(StockMovementRepository.class);
        GetKiosqueroDashboard useCase = useCase(
                purchaseRepository,
                productRepository,
                stockMovementRepository
        );

        when(purchaseRepository.findByBuffetAndDateRange(eq(buffetId), any(), any()))
                .thenReturn(List.of(pending));
        when(productRepository.findAllActiveWithStock(buffetId)).thenReturn(List.of());
        when(stockMovementRepository.findByBuffetAndDateRange(eq(buffetId), any(), any()))
                .thenReturn(List.of());

        KiosqueroDashboard dashboard = useCase.execute(buffetId, date);

        assertEquals(BigDecimal.ZERO, dashboard.summary().averageTicket());
        assertFalse(dashboard.alerts().items().stream()
                .anyMatch(alert -> "Creditos devueltos".equals(alert.label())));
    }

    private Producto product(String name, String price) {
        Producto product = new Producto();
        product.setId(UUID.randomUUID());
        product.setNombre(name);
        product.setPrecio(new BigDecimal(price));
        return product;
    }

    private Producto product(String name, String price, String categoryName) {
        Producto product = product(name, price);
        product.setCategoria(category(categoryName));
        return product;
    }

    private CategoriaProducto category(String name) {
        CategoriaProducto category = new CategoriaProducto();
        category.setId(UUID.randomUUID());
        category.setDescripcion(name);
        return category;
    }

    private Producto inventoryProduct(
            Producto source,
            int stockActual,
            int stockReservado,
            int stockDisponible,
            int stockMinimo,
            EstadoInventario estado
    ) {
        Producto product = product(source.getNombre(), source.getPrecio().toPlainString());
        product.setId(source.getId());
        product.setStockActual(stockActual);
        product.setStockReservado(stockReservado);
        product.setStockDisponible(stockDisponible);
        product.setStockMinimo(stockMinimo);
        product.setEstadoInventario(estado);
        product.setTipoManejoInventario(TipoManejoInventario.STOCK_EXACTO);
        return product;
    }

    private Purchase purchase(
            EstadoCompra status,
            TipoCompra type,
            Producto product,
            int quantity,
            String total,
            LocalDateTime date,
            String slot
    ) {
        Purchase purchase = new Purchase();
        purchase.setId(UUID.randomUUID());
        purchase.setStatus(status);
        purchase.setTipo(type);
        purchase.setTotalAmount(new BigDecimal(total));
        purchase.setDate(date);
        purchase.setPickupSlotDescription(slot);
        setPickupSlotData(purchase, slot);
        purchase.setItems(Map.of(product, quantity));
        return purchase;
    }

    private Purchase purchaseWithItems(
            EstadoCompra status,
            TipoCompra type,
            Map<Producto, Integer> items,
            String total,
            LocalDateTime date,
            String slot
    ) {
        Purchase purchase = new Purchase();
        purchase.setId(UUID.randomUUID());
        purchase.setStatus(status);
        purchase.setTipo(type);
        purchase.setTotalAmount(total == null ? null : new BigDecimal(total));
        purchase.setDate(date);
        purchase.setPickupSlotDescription(slot);
        setPickupSlotData(purchase, slot);
        purchase.setItems(items);
        return purchase;
    }

    private Map<Producto, Integer> invalidItems(Producto product) {
        Map<Producto, Integer> items = new LinkedHashMap<>();
        items.put(product, 0);
        return items;
    }

    private void setPickupSlotData(Purchase purchase, String slot) {
        if (slot == null) {
            return;
        }

        purchase.setPickupSlotId(UUID.randomUUID());
        purchase.setPickupSlotStart(slotStart(slot));
        purchase.setPickupSlotEnd(slotEnd(slot));
    }

    private LocalTime slotStart(String slot) {
        return "Segundo recreo".equals(slot) ? LocalTime.of(10, 30) : LocalTime.of(9, 30);
    }

    private LocalTime slotEnd(String slot) {
        return "Segundo recreo".equals(slot) ? LocalTime.of(10, 45) : LocalTime.of(9, 45);
    }

    private MovimientoStock movement(
            TipoMovimientoStock type,
            Integer quantity,
            Integer newQuantity
    ) {
        return new MovimientoStock(
                UUID.randomUUID(),
                UUID.randomUUID(),
                type,
                quantity,
                4,
                newQuantity,
                "Movimiento operativo",
                null,
                null,
                LocalDateTime.now()
        );
    }

    private GetKiosqueroDashboard useCase(
            PurchaseRepository purchaseRepository,
            ProductRepository productRepository,
            StockMovementRepository stockMovementRepository
    ) {
        return new GetKiosqueroDashboard(
                purchaseRepository,
                productRepository,
                stockMovementRepository,
                Clock.fixed(
                        Instant.parse("2026-06-10T15:00:00Z"),
                        ZoneId.of("America/Argentina/Buenos_Aires")
                )
        );
    }
}
