package com.recred.backend.unit.application;

import com.recred.backend.application.ListKiosqueroOperationalAlerts;
import com.recred.backend.application.ListKiosqueroOperationalAlertsRequest;
import com.recred.backend.application.ListKiosqueroOperationalAlertsResponse;
import com.recred.backend.application.OperationalAlertResponse;
import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.OperationalAlertType;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.TipoManejoInventario;
import com.recred.backend.domain.repositories.ProductRepository;
import com.recred.backend.domain.repositories.PurchaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListKiosqueroOperationalAlertsTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-05-27T12:50:00Z"),
            ZoneId.of("America/Argentina/Buenos_Aires")
    );

    @Mock
    private PurchaseRepository purchaseRepository;

    @Mock
    private ProductRepository productRepository;

    private ListKiosqueroOperationalAlerts useCase;

    private UUID buffetId;
    private Producto product;
    private Purchase order;
    private ListKiosqueroOperationalAlertsResponse response;

    @BeforeEach
    void setUp() {
        useCase = new ListKiosqueroOperationalAlerts(
                purchaseRepository,
                productRepository,
                CLOCK
        );
    }

    @Test
    void givenManyPendingOrdersAndLowCapacity_whenListingAlerts_thenReturnsOperationalAlerts() {
        givenOperationalAlertsScenario();
        givenManyPendingOrdersNearPickup();
        givenProductHasLowDailyCapacity();

        whenListingOperationalAlerts();

        thenAlertIsReturned(OperationalAlertType.MANY_PENDING_ORDERS);
        thenAlertIsReturned(OperationalAlertType.ORDER_UPCOMING_PICKUP);
        thenAlertIsReturned(OperationalAlertType.DAILY_CAPACITY_LOW);
    }

    @Test
    void givenAccumulatedExpiredOrders_whenListingAlerts_thenReturnsAccumulationAndExpiredAlerts() {
        givenOperationalAlertsScenario();
        givenOrdersAreAccumulatedAndExpired();
        givenNoActiveProducts();

        whenListingOperationalAlerts();

        thenAlertIsReturned(OperationalAlertType.ORDER_ACCUMULATION);
        thenAlertIsReturned(OperationalAlertType.ORDER_EXPIRED);
        thenAlertIsNotReturned(OperationalAlertType.MANY_PENDING_ORDERS);
    }

    @Test
    void givenMissingPickupStartAndInvalidCapacity_whenListingAlerts_thenSkipsThoseAlerts() {
        givenOperationalAlertsScenario();
        givenOrderHasNoPickupStart();
        givenProductHasInvalidDailyCapacity();

        whenListingOperationalAlerts();

        thenAlertIsNotReturned(OperationalAlertType.ORDER_UPCOMING_PICKUP);
        thenAlertIsNotReturned(OperationalAlertType.DAILY_CAPACITY_LOW);
    }

    private void givenOperationalAlertsScenario() {
        buffetId = UUID.randomUUID();
    }

    private void givenManyPendingOrdersNearPickup() {
        product = preparedProduct();
        order = pendingOrder(buffetId, product);
        when(purchaseRepository.findByBuffetAndStatuses(eq(buffetId), anyList(), eq(100)))
                .thenReturn(List.of(order, order, order, order, order));
    }

    private void givenProductHasLowDailyCapacity() {
        when(productRepository.findAllActiveWithStock(buffetId)).thenReturn(List.of(product));
        when(purchaseRepository.sumScheduledProductQuantity(
                eq(buffetId),
                eq(product.getId()),
                eq(LocalDate.of(2026, 5, 27)),
                anyList()
        )).thenReturn(4L);
    }

    private void givenOrdersAreAccumulatedAndExpired() {
        Purchase expiredOrder = order(
                buffetId,
                EstadoCompra.EN_PREPARACION,
                LocalDate.of(2026, 5, 27),
                LocalTime.of(9, 0),
                LocalTime.of(9, 15)
        );
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
    }

    private void givenNoActiveProducts() {
        when(productRepository.findAllActiveWithStock(buffetId)).thenReturn(List.of());
    }

    private void givenOrderHasNoPickupStart() {
        order = order(
                buffetId,
                EstadoCompra.LISTO,
                LocalDate.of(2026, 5, 27),
                null,
                null
        );
        when(purchaseRepository.findByBuffetAndStatuses(eq(buffetId), anyList(), eq(100)))
                .thenReturn(List.of(order));
    }

    private void givenProductHasInvalidDailyCapacity() {
        product = preparedProduct();
        product.setCupoMaximoDiario(0);
        when(productRepository.findAllActiveWithStock(buffetId)).thenReturn(List.of(product));
    }

    private void whenListingOperationalAlerts() {
        response = useCase.execute(new ListKiosqueroOperationalAlertsRequest(buffetId));
    }

    private void thenAlertIsReturned(OperationalAlertType type) {
        assertTrue(hasAlert(response.alerts(), type));
    }

    private void thenAlertIsNotReturned(OperationalAlertType type) {
        assertFalse(hasAlert(response.alerts(), type));
    }

    private boolean hasAlert(
            List<OperationalAlertResponse> alerts,
            OperationalAlertType type
    ) {
        return alerts.stream().anyMatch(alert -> alert.type().equals(type.name()));
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

    private Producto preparedProduct() {
        Producto product = new Producto();
        product.setId(UUID.randomUUID());
        product.setNombre("Sandwich");
        product.setTipoManejoInventario(TipoManejoInventario.CUPO_DIARIO);
        product.setCupoMaximoDiario(5);
        return product;
    }
}
