package com.recred.backend.application;

import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.TipoCompra;
import com.recred.backend.domain.repositories.PurchaseRepository;
import com.recred.backend.domain.services.InventoryAvailabilityService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExpireAdvancePurchasesTest {

    @Test
    void execute_ShouldExpirePurchasesAndReleaseReservations() {
        PurchaseRepository purchaseRepository = mock(PurchaseRepository.class);
        InventoryAvailabilityService inventoryAvailabilityService =
                mock(InventoryAvailabilityService.class);
        ExpireAdvancePurchases useCase = new ExpireAdvancePurchases(
                purchaseRepository,
                inventoryAvailabilityService
        );
        LocalDateTime now = LocalDateTime.of(2026, 5, 27, 11, 0);
        Purchase purchase = activeAdvancePurchase(EstadoCompra.LISTO);

        when(purchaseRepository.findExpiredAdvancePurchases(
                eq(now.toLocalDate()),
                eq(now.toLocalTime()),
                anyList()
        )).thenReturn(List.of(purchase));

        int expired = useCase.execute(now);

        assertEquals(1, expired);
        assertEquals(EstadoCompra.VENCIDO, purchase.getStatus());
        verify(inventoryAvailabilityService).releaseReservation(
                purchase.getBuffetId(),
                purchase.getItems(),
                purchase.getId()
        );
        verify(purchaseRepository).save(purchase);
    }

    private Purchase activeAdvancePurchase(EstadoCompra status) {
        Producto product = new Producto();
        product.setId(UUID.randomUUID());
        Purchase purchase = new Purchase();
        purchase.setId(UUID.randomUUID());
        purchase.setTipo(TipoCompra.ANTICIPADA);
        purchase.setStatus(status);
        purchase.setBuffetId(UUID.randomUUID());
        purchase.setItems(Map.of(product, 1));
        return purchase;
    }
}
