package com.recred.backend.domain.models;

import com.recred.backend.domain.exceptions.InvalidPurchaseStatusTransitionException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PurchaseTest {

    @Test
    void constructor_WhenProductRequiresPreparation_ShouldStartAsPending() {
        Purchase purchase = new Purchase(
                new Student(),
                Map.of(product(true), 1),
                PaymentMethod.CREDITOS
        );

        assertEquals(PurchaseState.PENDIENTE, purchase.getStatus());
    }

    @Test
    void constructor_WhenNoProductRequiresPreparation_ShouldStartAsDelivered() {
        Purchase purchase = new Purchase(
                new Student(),
                Map.of(product(false), 1),
                PaymentMethod.CREDITOS
        );

        assertEquals(PurchaseState.ENTREGADO, purchase.getStatus());
    }

    @Test
    void constructor_WhenProductPriceIsNull_ShouldCalculateZeroTotal() {
        Product product = product(false);
        product.setPrice(null);

        Purchase purchase = new Purchase(
                new Student(),
                Map.of(product, 2),
                PaymentMethod.CREDITOS
        );

        assertEquals(BigDecimal.ZERO, purchase.getTotalAmount());
    }

    @Test
    void changeStatusTo_WhenTransitionIsValid_ShouldUpdateStatus() {
        Purchase purchase = purchaseWithStatus(PurchaseState.PENDIENTE);

        purchase.changeStatusTo(PurchaseState.EN_PREPARACION);
        purchase.changeStatusTo(PurchaseState.LISTO);

        assertEquals(PurchaseState.LISTO, purchase.getStatus());
    }

    @Test
    void changeStatusTo_WhenTransitionIsInvalid_ShouldThrowException() {
        Purchase purchase = purchaseWithStatus(PurchaseState.PENDIENTE);

        assertThrows(InvalidPurchaseStatusTransitionException.class,
                () -> purchase.changeStatusTo(PurchaseState.LISTO));
    }

    @Test
    void changeStatusTo_WhenPendingIsCancelled_ShouldUpdateStatus() {
        Purchase purchase = purchaseWithStatus(PurchaseState.PENDIENTE);

        purchase.changeStatusTo(PurchaseState.CANCELADO);

        assertEquals(PurchaseState.CANCELADO, purchase.getStatus());
    }

    @Test
    void changeStatusTo_WhenPreparationIsCancelled_ShouldUpdateStatus() {
        Purchase purchase = purchaseWithStatus(PurchaseState.EN_PREPARACION);

        purchase.changeStatusTo(PurchaseState.CANCELADO);

        assertEquals(PurchaseState.CANCELADO, purchase.getStatus());
    }

    @Test
    void changeStatusTo_WhenPreparationReceivesInvalidStatus_ShouldThrowException() {
        Purchase purchase = purchaseWithStatus(PurchaseState.EN_PREPARACION);

        assertThrows(InvalidPurchaseStatusTransitionException.class,
                () -> purchase.changeStatusTo(PurchaseState.ENTREGADO));
    }

    @Test
    void changeStatusTo_WhenReadyIsDelivered_ShouldUpdateStatus() {
        Purchase purchase = purchaseWithStatus(PurchaseState.LISTO);

        purchase.changeStatusTo(PurchaseState.ENTREGADO);

        assertEquals(PurchaseState.ENTREGADO, purchase.getStatus());
    }

    @Test
    void changeStatusTo_WhenReadyIsCancelled_ShouldUpdateStatus() {
        Purchase purchase = purchaseWithStatus(PurchaseState.LISTO);

        purchase.changeStatusTo(PurchaseState.CANCELADO);

        assertEquals(PurchaseState.CANCELADO, purchase.getStatus());
    }

    @Test
    void changeStatusTo_WhenReadyReceivesInvalidStatus_ShouldThrowException() {
        Purchase purchase = purchaseWithStatus(PurchaseState.LISTO);

        assertThrows(InvalidPurchaseStatusTransitionException.class,
                () -> purchase.changeStatusTo(PurchaseState.PENDIENTE));
    }

    @Test
    void changeStatusTo_WhenDelivered_ShouldThrowException() {
        Purchase purchase = purchaseWithStatus(PurchaseState.ENTREGADO);

        assertThrows(InvalidPurchaseStatusTransitionException.class,
                () -> purchase.changeStatusTo(PurchaseState.LISTO));
    }

    @Test
    void changeStatusTo_WhenCancelled_ShouldThrowException() {
        Purchase purchase = purchaseWithStatus(PurchaseState.CANCELADO);

        assertThrows(InvalidPurchaseStatusTransitionException.class,
                () -> purchase.changeStatusTo(PurchaseState.ENTREGADO));
    }

    @Test
    void changeStatusTo_WhenCurrentStatusIsNull_ShouldThrowException() {
        Purchase purchase = purchaseWithStatus(null);

        assertThrows(InvalidPurchaseStatusTransitionException.class,
                () -> purchase.changeStatusTo(PurchaseState.LISTO));
    }

    @Test
    void changeStatusTo_WhenNextStatusIsNull_ShouldThrowException() {
        Purchase purchase = purchaseWithStatus(PurchaseState.PENDIENTE);

        assertThrows(InvalidPurchaseStatusTransitionException.class,
                () -> purchase.changeStatusTo(null));
    }

    @Test
    void cancel_WhenPurchaseIsAlreadyCancelled_ShouldThrowException() {
        Purchase purchase = purchaseWithStatus(PurchaseState.CANCELADO);

        assertThrows(IllegalStateException.class, purchase::cancel);
    }

    private Purchase purchaseWithStatus(PurchaseState status) {
        Purchase purchase = new Purchase();
        purchase.setStatus(status);
        return purchase;
    }

    private Product product(Boolean requiresPreparation) {
        Product product = new Product();
        product.setPrice(new BigDecimal("10.00"));
        product.setRequiresPreparation(requiresPreparation);
        return product;
    }
}
