package com.recred.backend.unit.application;

import com.recred.backend.application.ExpireAdvancePurchases;
import com.recred.backend.application.ExpireAdvancePurchasesRequest;
import com.recred.backend.application.ExpireAdvancePurchasesResponse;
import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.repositories.PurchaseRepository;
import com.recred.backend.domain.services.InventoryAvailabilityService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.recred.backend.domain.models.EstadoCompra.EN_PREPARACION;
import static com.recred.backend.domain.models.EstadoCompra.LISTO;
import static com.recred.backend.domain.models.EstadoCompra.PENDIENTE;
import static com.recred.backend.domain.models.EstadoCompra.VENCIDO;
import static com.recred.backend.domain.models.TipoCompra.ANTICIPADA;
import static com.recred.backend.domain.models.TipoCompra.PRESENCIAL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExpireAdvancePurchasesTest {

    private static final UUID BUFFET_ID = UUID.randomUUID();
    private static final UUID PURCHASE_ID = UUID.randomUUID();
    private static final UUID PRODUCT_ID = UUID.randomUUID();
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 27, 11, 0);

    private final PurchaseRepository purchaseRepository = mock(PurchaseRepository.class);
    private final InventoryAvailabilityService inventoryAvailabilityService = mock(InventoryAvailabilityService.class);
    private final ExpireAdvancePurchases useCase = new ExpireAdvancePurchases(purchaseRepository, inventoryAvailabilityService);

    @Test
    void expirePurchasesAndReleaseReservationsSuccessfully() {
        Purchase purchase = activeAdvancePurchase(LISTO);
        givenExpiredAdvancePurchasesFound(purchase);

        ExpireAdvancePurchasesResponse result = whenExecuteUseCase(new ExpireAdvancePurchasesRequest(null, NOW));

        thenPurchasesShouldBeExpiredAndReservationsReleased(result, purchase);
    }

    @Test
    void expireByBuffetAndIgnoreNullQuantities() {
        Purchase purchase = activeAdvancePurchase(EN_PREPARACION);
        purchase.setBuffetId(BUFFET_ID);
        purchase.setItems(itemsWithNullQuantity());
        givenExpiredAdvancePurchasesFoundByBuffet(purchase);

        ExpireAdvancePurchasesResponse result = whenExecuteUseCase(new ExpireAdvancePurchasesRequest(BUFFET_ID, NOW));

        thenPurchasesShouldBeExpiredAndTwoReservationsReleased(result, purchase);
    }

    @Test
    void notReleaseReservationWhenPurchaseIsNotAdvance() {
        Purchase purchase = activeAdvancePurchase(PENDIENTE);
        purchase.setBuffetId(BUFFET_ID);
        purchase.setTipo(PRESENCIAL);
        givenExpiredAdvancePurchasesFoundByBuffet(purchase);

        ExpireAdvancePurchasesResponse result = whenExecuteUseCase(new ExpireAdvancePurchasesRequest(BUFFET_ID, NOW));

        thenPurchaseShouldBeExpiredButNoReservationsReleased(result, purchase);
    }

    private void givenExpiredAdvancePurchasesFound(Purchase purchase) {
        when(purchaseRepository.findExpiredAdvancePurchases(
                eq(NOW.toLocalDate()),
                eq(NOW.toLocalTime()),
                anyList()
        )).thenReturn(List.of(purchase));
    }

    private void givenExpiredAdvancePurchasesFoundByBuffet(Purchase purchase) {
        when(purchaseRepository.findExpiredAdvancePurchases(
                eq(BUFFET_ID),
                eq(NOW.toLocalDate()),
                eq(NOW.toLocalTime()),
                anyList()
        )).thenReturn(List.of(purchase));
    }

    private ExpireAdvancePurchasesResponse whenExecuteUseCase(ExpireAdvancePurchasesRequest request) {
        return useCase.execute(request);
    }

    private void thenPurchasesShouldBeExpiredAndReservationsReleased(ExpireAdvancePurchasesResponse result, Purchase purchase) {
        assertEquals(1, result.expiredPurchases());
        assertEquals(VENCIDO, purchase.getStatus());
        verify(inventoryAvailabilityService).releaseReservation(
                purchase.getBuffetId(),
                purchase.getItems(),
                purchase.getId()
        );
        verify(purchaseRepository).save(purchase);
    }

    private void thenPurchasesShouldBeExpiredAndTwoReservationsReleased(ExpireAdvancePurchasesResponse result, Purchase purchase) {
        assertEquals(1, result.expiredPurchases());
        assertEquals(2, result.releasedReservations());
        assertEquals(VENCIDO, purchase.getStatus());
        verify(inventoryAvailabilityService).releaseReservation(
                purchase.getBuffetId(),
                purchase.getItems(),
                purchase.getId()
        );
        verify(purchaseRepository).save(purchase);
    }

    private void thenPurchaseShouldBeExpiredButNoReservationsReleased(ExpireAdvancePurchasesResponse result, Purchase purchase) {
        assertEquals(1, result.expiredPurchases());
        assertEquals(VENCIDO, purchase.getStatus());
        verify(inventoryAvailabilityService, never()).releaseReservation(
                purchase.getBuffetId(),
                purchase.getItems(),
                purchase.getId()
        );
        verify(purchaseRepository).save(purchase);
    }

    private Map<Producto, Integer> itemsWithNullQuantity() {
        Map<Producto, Integer> items = new HashMap<>();
        items.put(product(), 2);
        items.put(product(), null);
        return items;
    }

    private Purchase activeAdvancePurchase(EstadoCompra status) {
        Purchase purchase = new Purchase();
        purchase.setId(PURCHASE_ID);
        purchase.setTipo(ANTICIPADA);
        purchase.setStatus(status);
        purchase.setBuffetId(BUFFET_ID);
        purchase.setItems(Map.of(product(), 1));
        return purchase;
    }

    private Producto product() {
        Producto product = new Producto();
        product.setId(PRODUCT_ID);
        return product;
    }
}
