package com.recred.backend.unit.application;

import com.recred.backend.application.GetPresentialPurchase;
import com.recred.backend.application.GetPresentialPurchaseRequest;
import com.recred.backend.application.GetPresentialPurchaseResponse;
import com.recred.backend.domain.exceptions.PurchaseNotFoundException;
import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.MedioPago;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.TipoCompra;
import com.recred.backend.domain.repositories.PurchaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetPresentialPurchaseTest {

    @Mock
    private PurchaseRepository purchaseRepository;

    private GetPresentialPurchase useCase;

    private UUID purchaseId;
    private Purchase purchase;
    private GetPresentialPurchaseResponse response;
    private RuntimeException thrownException;

    @BeforeEach
    void setUp() {
        useCase = new GetPresentialPurchase(purchaseRepository);
    }

    @Test
    void givenExistingPurchase_whenGettingPresentialPurchase_thenReturnsPurchase() {
        givenPurchaseScenario();
        givenPurchaseExists();

        whenGettingPresentialPurchase();

        thenPurchaseIsReturned();
        thenPurchaseStatusIsReturned();
    }

    @Test
    void givenMissingPurchase_whenGettingPresentialPurchase_thenFails() {
        givenPurchaseScenario();
        givenPurchaseDoesNotExist();

        whenGettingPresentialPurchaseFails();

        thenPurchaseNotFoundIsThrown();
    }

    @Test
    void givenPurchaseWithRegularAndPromotionItems_whenGettingPresentialPurchase_thenGroupsPromotionItems() {
        givenPurchaseScenario();
        givenPurchaseHasRegularAndPromotionItems();
        givenPurchaseExists();

        whenGettingPresentialPurchase();

        thenPurchaseDetailsAreReturned();
        thenRegularItemIsReturned();
        thenPromotionItemsAreGrouped();
    }

    @Test
    void givenPurchaseWithNullableFields_whenGettingPresentialPurchase_thenReturnsNullableFields() {
        givenPurchaseScenario();
        givenPurchaseHasNullableFields();
        givenPurchaseExists();

        whenGettingPresentialPurchase();

        thenNullablePurchaseFieldsAreReturned();
        thenItemsAreEmpty();
    }

    private void givenPurchaseScenario() {
        purchaseId = UUID.randomUUID();
        purchase = new Purchase();
        purchase.setId(purchaseId);
        purchase.setStatus(EstadoCompra.PENDIENTE);
    }

    private void givenPurchaseExists() {
        when(purchaseRepository.findById(purchaseId)).thenReturn(Optional.of(purchase));
    }

    private void givenPurchaseDoesNotExist() {
        when(purchaseRepository.findById(purchaseId)).thenReturn(Optional.empty());
    }

    private void givenPurchaseHasRegularAndPromotionItems() {
        UUID promotionId = UUID.randomUUID();
        Producto standalone = product("Agua", new BigDecimal("700.00"), false, null);
        Producto firstPromotionItem = product(
                "Cafe",
                new BigDecimal("800.00"),
                true,
                promotionId);
        Producto secondPromotionItem = product(
                "Medialuna",
                new BigDecimal("600.00"),
                false,
                promotionId);

        purchase.setTotalAmount(new BigDecimal("2100.00"));
        purchase.setPaymentMethod(MedioPago.CREDITOS);
        purchase.setDate(LocalDateTime.of(2026, 7, 1, 10, 0));
        purchase.setTipo(TipoCompra.ANTICIPADA);
        purchase.setWithdrawalCode("RET-123");
        purchase.setPickupSlotDescription("Primer recreo");
        purchase.setPickupDate(LocalDate.of(2026, 7, 2));
        purchase.setPickupSlotStartTime(LocalTime.of(9, 0));
        purchase.setItems(Map.of(
                standalone, 1,
                firstPromotionItem, 2,
                secondPromotionItem, 2
        ));
    }

    private void givenPurchaseHasNullableFields() {
        purchase.setStatus(null);
        purchase.setItems(null);
    }

    private void whenGettingPresentialPurchase() {
        response = useCase.execute(new GetPresentialPurchaseRequest(purchaseId));
    }

    private void whenGettingPresentialPurchaseFails() {
        thrownException = assertThrows(RuntimeException.class, () ->
                useCase.execute(new GetPresentialPurchaseRequest(purchaseId)));
    }

    private void thenPurchaseIsReturned() {
        assertEquals(purchaseId, response.id());
    }

    private void thenPurchaseStatusIsReturned() {
        assertEquals("PENDIENTE", response.status());
    }

    private void thenPurchaseNotFoundIsThrown() {
        assertTrue(thrownException instanceof PurchaseNotFoundException);
    }

    private void thenPurchaseDetailsAreReturned() {
        assertEquals("CREDITOS", response.paymentMethod());
        assertEquals("ANTICIPADA", response.tipo());
        assertEquals("RET-123", response.withdrawalCode());
        assertEquals(LocalDate.of(2026, 7, 2), response.pickupDate());
        assertEquals(LocalTime.of(9, 0), response.pickupSlotStartTime());
    }

    private void thenRegularItemIsReturned() {
        GetPresentialPurchaseResponse.ItemResponse regularItem = itemNamed("Agua");
        assertEquals(new BigDecimal("700.00"), regularItem.precio());
        assertEquals(1, regularItem.quantity());
        assertNull(regularItem.promocionId());
    }

    private void thenPromotionItemsAreGrouped() {
        GetPresentialPurchaseResponse.ItemResponse promotionItem = itemNamed("Promo desayuno");
        assertEquals(2, response.items().size());
        assertEquals(2, promotionItem.quantity());
        assertEquals(new BigDecimal("1400.00"), promotionItem.precio());
        assertTrue(promotionItem.requierePreparacion());
        assertEquals(promotionItem.productId(), promotionItem.promocionId());
    }

    private void thenNullablePurchaseFieldsAreReturned() {
        assertEquals(purchaseId, response.id());
        assertNull(response.status());
        assertNull(response.statusLabel());
        assertNull(response.paymentMethod());
    }

    private void thenItemsAreEmpty() {
        assertEquals(0, response.items().size());
    }

    private GetPresentialPurchaseResponse.ItemResponse itemNamed(String name) {
        return response.items().stream()
                .filter(item -> item.nombre().equals(name))
                .findFirst()
                .orElseThrow();
    }

    private Producto product(
            String name,
            BigDecimal price,
            boolean requiresPreparation,
            UUID promotionId
    ) {
        Producto product = new Producto();
        product.setId(UUID.randomUUID());
        product.setNombre(name);
        product.setPrecio(price);
        product.setRequierePreparacion(requiresPreparation);
        product.setCupoMaximoDiario(10);
        product.setPromocionId(promotionId);
        product.setPromocionNombre(promotionId == null ? null : "Promo desayuno");
        return product;
    }
}
