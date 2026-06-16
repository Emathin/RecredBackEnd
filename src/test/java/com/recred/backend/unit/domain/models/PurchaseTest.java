package com.recred.backend.unit.domain.models;

import com.recred.backend.domain.exceptions.InvalidPurchaseStatusTransitionException;
import com.recred.backend.domain.models.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PurchaseTest {

    @Test
    void constructor_WhenProductRequiresPreparation_ShouldStartAsPending() {
        Purchase purchase = new Purchase(
                new Alumno(),
                Map.of(product(true), 1),
                MedioPago.CREDITOS,
                UUID.randomUUID(),
                BuyerType.STUDENT
        );

        assertEquals(EstadoCompra.PENDIENTE, purchase.getStatus());
    }

    @Test
    void constructor_WhenNoProductRequiresPreparation_ShouldStartAsDelivered() {
        Purchase purchase = new Purchase(
                new Alumno(),
                Map.of(product(false), 1),
                MedioPago.CREDITOS,
                UUID.randomUUID(),
                BuyerType.STUDENT
        );

        assertEquals(EstadoCompra.ENTREGADO, purchase.getStatus());
    }

    @Test
    void constructor_WhenProductPriceIsNull_ShouldCalculateZeroTotal() {
        Producto product = product(false);
        product.setPrecio(null);

        Purchase purchase = new Purchase(
                new Alumno(),
                Map.of(product, 2),
                MedioPago.CREDITOS,
                UUID.randomUUID(),
                BuyerType.STUDENT
        );

        assertEquals(BigDecimal.ZERO, purchase.getTotalAmount());
    }

    @Test
    void changeStatusTo_WhenTransitionIsValid_ShouldUpdateStatus() {
        Purchase purchase = purchaseWithStatus(EstadoCompra.PENDIENTE);
        purchase.setTipo(TipoCompra.ANTICIPADA);

        purchase.changeStatusTo(EstadoCompra.EN_PREPARACION);
        assertEquals(EstadoRetiro.EN_PREPARACION, purchase.getWithdrawalStatus());

        purchase.changeStatusTo(EstadoCompra.LISTO);

        assertEquals(EstadoCompra.LISTO, purchase.getStatus());
        assertEquals(EstadoRetiro.LISTO, purchase.getWithdrawalStatus());
    }

    @Test
    void changeStatusTo_WhenTransitionIsInvalid_ShouldThrowException() {
        Purchase purchase = purchaseWithStatus(EstadoCompra.PENDIENTE);

        assertThrows(InvalidPurchaseStatusTransitionException.class,
                () -> purchase.changeStatusTo(EstadoCompra.LISTO));
    }

    @Test
    void changeStatusTo_WhenPendingIsCancelled_ShouldUpdateStatus() {
        Purchase purchase = purchaseWithStatus(EstadoCompra.PENDIENTE);

        purchase.changeStatusTo(EstadoCompra.CANCELADO);

        assertEquals(EstadoCompra.CANCELADO, purchase.getStatus());
    }

    @Test
    void changeStatusTo_WhenPendingIsRejected_ShouldUpdateStatus() {
        Purchase purchase = purchaseWithStatus(EstadoCompra.PENDIENTE);

        purchase.changeStatusTo(EstadoCompra.RECHAZADO);

        assertEquals(EstadoCompra.RECHAZADO, purchase.getStatus());
    }

    @Test
    void changeStatusTo_WhenPendingIsExpired_ShouldUpdateStatus() {
        Purchase purchase = purchaseWithStatus(EstadoCompra.PENDIENTE);

        purchase.changeStatusTo(EstadoCompra.VENCIDO);

        assertEquals(EstadoCompra.VENCIDO, purchase.getStatus());
    }

    @Test
    void changeStatusTo_WhenPreparationIsCancelled_ShouldUpdateStatus() {
        Purchase purchase = purchaseWithStatus(EstadoCompra.EN_PREPARACION);

        purchase.changeStatusTo(EstadoCompra.CANCELADO);

        assertEquals(EstadoCompra.CANCELADO, purchase.getStatus());
    }

    @Test
    void changeStatusTo_WhenPreparationReceivesInvalidStatus_ShouldThrowException() {
        Purchase purchase = purchaseWithStatus(EstadoCompra.EN_PREPARACION);

        assertThrows(InvalidPurchaseStatusTransitionException.class,
                () -> purchase.changeStatusTo(EstadoCompra.ENTREGADO));
    }

    @Test
    void changeStatusTo_WhenReadyIsDelivered_ShouldUpdateStatus() {
        Purchase purchase = purchaseWithStatus(EstadoCompra.LISTO);

        purchase.changeStatusTo(EstadoCompra.ENTREGADO);

        assertEquals(EstadoCompra.ENTREGADO, purchase.getStatus());
    }

    @Test
    void changeStatusTo_WhenReadyIsCancelled_ShouldThrowException() {
        Purchase purchase = purchaseWithStatus(EstadoCompra.LISTO);

        assertThrows(InvalidPurchaseStatusTransitionException.class,
                () -> purchase.changeStatusTo(EstadoCompra.CANCELADO));
    }

    @Test
    void changeStatusTo_WhenReadyIsNoRetirado_ShouldUpdateStatus() {
        Purchase purchase = purchaseWithStatus(EstadoCompra.LISTO);
        purchase.setTipo(TipoCompra.ANTICIPADA);

        purchase.changeStatusTo(EstadoCompra.NO_RETIRADO);

        assertEquals(EstadoCompra.NO_RETIRADO, purchase.getStatus());
        assertEquals(EstadoRetiro.NO_RETIRADO, purchase.getWithdrawalStatus());
    }

    @Test
    void changeStatusTo_WhenReadyReceivesInvalidStatus_ShouldThrowException() {
        Purchase purchase = purchaseWithStatus(EstadoCompra.LISTO);

        assertThrows(InvalidPurchaseStatusTransitionException.class,
                () -> purchase.changeStatusTo(EstadoCompra.PENDIENTE));
    }

    @Test
    void changeStatusTo_WhenDelivered_ShouldThrowException() {
        Purchase purchase = purchaseWithStatus(EstadoCompra.ENTREGADO);

        assertThrows(InvalidPurchaseStatusTransitionException.class,
                () -> purchase.changeStatusTo(EstadoCompra.LISTO));
    }

    @Test
    void changeStatusTo_WhenCancelled_ShouldThrowException() {
        Purchase purchase = purchaseWithStatus(EstadoCompra.CANCELADO);

        assertThrows(InvalidPurchaseStatusTransitionException.class,
                () -> purchase.changeStatusTo(EstadoCompra.ENTREGADO));
    }

    @Test
    void changeStatusTo_WhenRejected_ShouldThrowException() {
        Purchase purchase = purchaseWithStatus(EstadoCompra.RECHAZADO);

        assertThrows(InvalidPurchaseStatusTransitionException.class,
                () -> purchase.changeStatusTo(EstadoCompra.ENTREGADO));
    }

    @Test
    void changeStatusTo_WhenCurrentStatusIsNull_ShouldThrowException() {
        Purchase purchase = purchaseWithStatus(null);

        assertThrows(InvalidPurchaseStatusTransitionException.class,
                () -> purchase.changeStatusTo(EstadoCompra.LISTO));
    }

    @Test
    void changeStatusTo_WhenNextStatusIsNull_ShouldThrowException() {
        Purchase purchase = purchaseWithStatus(EstadoCompra.PENDIENTE);

        assertThrows(InvalidPurchaseStatusTransitionException.class,
                () -> purchase.changeStatusTo(null));
    }

    @Test
    void cancel_WhenPurchaseIsAlreadyCancelled_ShouldThrowException() {
        Purchase purchase = purchaseWithStatus(EstadoCompra.CANCELADO);

        assertThrows(IllegalStateException.class, purchase::cancel);
    }

    @Test
    void cancel_WhenPurchaseIsNotPendingOrInPreparation_ShouldThrowException() {
        Purchase purchase = purchaseWithStatus(EstadoCompra.LISTO);

        assertThrows(IllegalStateException.class, purchase::cancel);
    }

    private Purchase purchaseWithStatus(EstadoCompra status) {
        Purchase purchase = new Purchase();
        purchase.setStatus(status);
        return purchase;
    }

    private Producto product(Boolean requiresPreparation) {
        Producto product = new Producto();
        product.setPrecio(new BigDecimal("10.00"));
        product.setRequierePreparacion(requiresPreparation);
        return product;
    }
}
