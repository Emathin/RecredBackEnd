package com.recred.backend.unit.application;

import com.recred.backend.application.ListKiosqueroOperationalAlerts;
import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.KiosqueroOperationalAlert;
import com.recred.backend.domain.models.OperationalAlertType;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.TipoManejoInventario;
import com.recred.backend.domain.repositories.ProductRepository;
import com.recred.backend.domain.repositories.PurchaseRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ListKiosqueroOperationalAlertsTest {

    @Test
    void execute_ShouldReturnOperationalAlerts() {
        UUID buffetId = UUID.randomUUID();
        PurchaseRepository purchaseRepository = mock(PurchaseRepository.class);
        ProductRepository productRepository = mock(ProductRepository.class);
        Producto product = preparedProduct();
        Purchase order = pendingOrder(buffetId, product);
        ListKiosqueroOperationalAlerts useCase = new ListKiosqueroOperationalAlerts(
                purchaseRepository,
                productRepository,
                Clock.fixed(
                        Instant.parse("2026-05-27T12:50:00Z"),
                        ZoneId.of("America/Argentina/Buenos_Aires")
                )
        );

        when(purchaseRepository.findByBuffetAndStatuses(eq(buffetId), anyList(), eq(100)))
                .thenReturn(List.of(order, order, order, order, order));
        when(productRepository.findAllActiveWithStock(buffetId)).thenReturn(List.of(product));
        when(purchaseRepository.sumScheduledProductQuantity(
                eq(buffetId),
                eq(product.getId()),
                eq(LocalDate.of(2026, 5, 27)),
                anyList()
        )).thenReturn(4L);

        List<KiosqueroOperationalAlert> alerts = useCase.execute(buffetId);

        assertTrue(hasAlert(alerts, OperationalAlertType.MANY_PENDING_ORDERS));
        assertTrue(hasAlert(alerts, OperationalAlertType.ORDER_UPCOMING_PICKUP));
        assertTrue(hasAlert(alerts, OperationalAlertType.DAILY_CAPACITY_LOW));
    }

    @Test
    void execute_WhenOrdersAreAccumulatedAndExpired_ShouldReturnBothAlerts() {
        UUID buffetId = UUID.randomUUID();
        PurchaseRepository purchaseRepository = mock(PurchaseRepository.class);
        ProductRepository productRepository = mock(ProductRepository.class);
        Purchase expiredOrder = order(
                buffetId,
                EstadoCompra.EN_PREPARACION,
                LocalDate.of(2026, 5, 27),
                LocalTime.of(9, 0),
                LocalTime.of(9, 15)
        );
        ListKiosqueroOperationalAlerts useCase = useCase(purchaseRepository, productRepository);

        when(purchaseRepository.findByBuffetAndStatuses(eq(buffetId), anyList(), eq(100)))
                .thenReturn(List.of(
                        expiredOrder,
                        activeOrder(buffetId),
                        activeOrder(buffetId),
                        activeOrder(buffetId),
                        activeOrder(buffetId),
                        activeOrder(buffetId),
                        activeOrder(buffetId),
                        activeOrder(buffetId)
                ));
        when(productRepository.findAllActiveWithStock(buffetId)).thenReturn(List.of());

        List<KiosqueroOperationalAlert> alerts = useCase.execute(buffetId);

        assertTrue(hasAlert(alerts, OperationalAlertType.ORDER_ACCUMULATION));
        assertTrue(hasAlert(alerts, OperationalAlertType.ORDER_EXPIRED));
        assertFalse(hasAlert(alerts, OperationalAlertType.MANY_PENDING_ORDERS));
    }

    @Test
    void execute_WhenPickupStartIsMissingAndCapacityIsInvalid_ShouldSkipThoseAlerts() {
        UUID buffetId = UUID.randomUUID();
        PurchaseRepository purchaseRepository = mock(PurchaseRepository.class);
        ProductRepository productRepository = mock(ProductRepository.class);
        Producto product = preparedProduct();
        product.setCupoMaximoDiario(0);
        Purchase orderWithoutStart = order(
                buffetId,
                EstadoCompra.LISTO,
                LocalDate.of(2026, 5, 27),
                null,
                null
        );
        ListKiosqueroOperationalAlerts useCase = useCase(purchaseRepository, productRepository);

        when(purchaseRepository.findByBuffetAndStatuses(eq(buffetId), anyList(), eq(100)))
                .thenReturn(List.of(orderWithoutStart));
        when(productRepository.findAllActiveWithStock(buffetId)).thenReturn(List.of(product));

        List<KiosqueroOperationalAlert> alerts = useCase.execute(buffetId);

        assertFalse(hasAlert(alerts, OperationalAlertType.ORDER_UPCOMING_PICKUP));
        assertFalse(hasAlert(alerts, OperationalAlertType.DAILY_CAPACITY_LOW));
    }

    private boolean hasAlert(
            List<KiosqueroOperationalAlert> alerts,
            OperationalAlertType type
    ) {
        return alerts.stream().anyMatch(alert -> alert.type() == type);
    }

    private Purchase pendingOrder(UUID buffetId, Producto product) {
        Purchase purchase = new Purchase();
        purchase.setId(UUID.randomUUID());
        purchase.setBuffetId(buffetId);
        purchase.setStatus(EstadoCompra.PENDIENTE);
        purchase.setPickupDate(LocalDate.of(2026, 5, 27));
        purchase.setPickupSlotStart(LocalTime.of(10, 0));
        purchase.setPickupSlotEnd(LocalTime.of(10, 20));
        purchase.setItems(Map.of(product, 1));
        return purchase;
    }

    private Purchase activeOrder(UUID buffetId) {
        return order(
                buffetId,
                EstadoCompra.LISTO,
                LocalDate.of(2026, 5, 28),
                LocalTime.of(11, 0),
                LocalTime.of(11, 20)
        );
    }

    private Purchase order(
            UUID buffetId,
            EstadoCompra status,
            LocalDate pickupDate,
            LocalTime pickupStart,
            LocalTime pickupEnd
    ) {
        Purchase purchase = new Purchase();
        purchase.setId(UUID.randomUUID());
        purchase.setBuffetId(buffetId);
        purchase.setStatus(status);
        purchase.setPickupDate(pickupDate);
        purchase.setPickupSlotStart(pickupStart);
        purchase.setPickupSlotEnd(pickupEnd);
        return purchase;
    }

    private ListKiosqueroOperationalAlerts useCase(
            PurchaseRepository purchaseRepository,
            ProductRepository productRepository
    ) {
        return new ListKiosqueroOperationalAlerts(
                purchaseRepository,
                productRepository,
                Clock.fixed(
                        Instant.parse("2026-05-27T12:50:00Z"),
                        ZoneId.of("America/Argentina/Buenos_Aires")
                )
        );
    }

    private Producto preparedProduct() {
        Producto product = new Producto();
        product.setId(UUID.randomUUID());
        product.setNombre("Sandwich");
        product.setTipoManejoInventario(TipoManejoInventario.CUPO_DIARIO);
        product.setCupoMaximoDiario(5);
        return product;
    }
}
