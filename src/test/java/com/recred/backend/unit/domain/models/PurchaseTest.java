package com.recred.backend.unit.domain.models;

import com.recred.backend.domain.exceptions.InvalidPurchaseStatusTransitionException;
import com.recred.backend.domain.models.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    void changeTrackingStatusTo_WhenRequestedStatusIsCancelled_ShouldThrowException() {
        Purchase purchase = purchaseWithStatus(EstadoCompra.PENDIENTE);

        assertThrows(InvalidPurchaseStatusTransitionException.class,
                () -> purchase.changeTrackingStatusTo(EstadoCompra.CANCELADO));
    }

    @Test
    void cancel_WhenPurchaseIsAlreadyCancelled_ShouldThrowException() {
        Purchase purchase = purchaseWithStatus(EstadoCompra.CANCELADO);

        assertThrows(IllegalStateException.class, 
                () -> purchase.cancel(UUID.randomUUID(), com.recred.backend.domain.models.RolUsuario.PADRE));
    }

    @Test
    void cancel_WhenPurchaseIsNotPendingOrInPreparation_ShouldThrowException() {
        Purchase purchase = purchaseWithStatus(EstadoCompra.LISTO);

        assertThrows(InvalidPurchaseStatusTransitionException.class, 
                () -> purchase.cancel(UUID.randomUUID(), com.recred.backend.domain.models.RolUsuario.PADRE));
    }

    @Test
    void revertCancellation_WhenPurchaseIsCancelled_ShouldUpdateStatus() {
        Purchase purchase = purchaseWithStatus(EstadoCompra.PENDIENTE);
        purchase.setTipo(TipoCompra.ANTICIPADA);

        UUID requesterId = UUID.randomUUID();
        purchase.cancel(requesterId, com.recred.backend.domain.models.RolUsuario.VENDEDOR);
        purchase.revertCancellation(requesterId, com.recred.backend.domain.models.RolUsuario.VENDEDOR);

        assertEquals(EstadoCompra.PENDIENTE, purchase.getStatus());
        assertEquals(EstadoRetiro.PROGRAMADO, purchase.getWithdrawalStatus());
    }

    @Test
    void revertCancellation_WhenPurchaseIsNotCancelled_ShouldThrowException() {
        Purchase purchase = purchaseWithStatus(EstadoCompra.PENDIENTE);

        assertThrows(IllegalStateException.class, 
                () -> purchase.revertCancellation(UUID.randomUUID(), com.recred.backend.domain.models.RolUsuario.PADRE));
    }

    @Test
    void dashboardStatusQueries_ShouldExposePurchaseBusinessState() {
        Purchase purchase = purchaseWithStatus(EstadoCompra.ENTREGADO);
        purchase.setTipo(TipoCompra.ANTICIPADA);

        assertEquals(true, purchase.hasStatus(EstadoCompra.ENTREGADO));
        assertEquals(true, purchase.hasType(TipoCompra.ANTICIPADA));
        assertEquals(true, purchase.isDelivered());
        assertEquals(true, purchase.isAdvancePurchase());
    }

    @Test
    void trackingSideEffectQueries_ShouldExposePurchaseBusinessRules() {
        Purchase purchase = purchaseWithStatus(EstadoCompra.PENDIENTE);
        purchase.setTipo(TipoCompra.ANTICIPADA);

        assertEquals(true, purchase.shouldConsumeReservationWhenChangingTo(
                EstadoCompra.ENTREGADO));
        assertEquals(true, purchase.shouldReleaseReservationWhenChangingFrom(
                EstadoCompra.PENDIENTE,
                EstadoCompra.RECHAZADO
        ));
        assertEquals(true, purchase.shouldRefundWhenChangingTo(EstadoCompra.RECHAZADO));
        assertEquals(true, purchase.shouldPublishExpirationEventFor(EstadoCompra.VENCIDO));
    }

    @Test
    void trackingSideEffectQueries_ShouldRejectPresentialPurchases() {
        Purchase purchase = purchaseWithStatus(EstadoCompra.PENDIENTE);
        purchase.setTipo(TipoCompra.PRESENCIAL);

        assertEquals(false, purchase.shouldConsumeReservationWhenChangingTo(
                EstadoCompra.ENTREGADO));
        assertEquals(false, purchase.shouldReleaseReservationWhenChangingFrom(
                EstadoCompra.PENDIENTE,
                EstadoCompra.RECHAZADO
        ));
        assertEquals(false, purchase.shouldRefundWhenChangingTo(EstadoCompra.RECHAZADO));
    }

    @Test
    void studentIdQueries_ShouldExposeAssociatedStudentId() {
        UUID studentId = UUID.randomUUID();
        Alumno student = new Alumno();
        student.setId(studentId);
        Purchase purchase = purchaseWithStatus(EstadoCompra.PENDIENTE);
        purchase.setStudent(student);

        assertEquals(true, purchase.hasStudentId());
        assertEquals(studentId, purchase.studentIdOrNull());
        assertEquals(studentId, purchase.requireStudentId());
    }

    @Test
    void requireStudentId_WhenStudentDoesNotExist_ShouldThrowException() {
        Purchase purchase = purchaseWithStatus(EstadoCompra.PENDIENTE);

        assertEquals(false, purchase.hasStudentId());
        assertThrows(IllegalStateException.class, purchase::requireStudentId);
    }

    @Test
    void refundsCredits_ShouldReturnTrueForCancelledAndRejectedPurchases() {
        Purchase cancelled = purchaseWithStatus(EstadoCompra.CANCELADO);
        Purchase rejected = purchaseWithStatus(EstadoCompra.RECHAZADO);
        Purchase delivered = purchaseWithStatus(EstadoCompra.ENTREGADO);

        assertEquals(true, cancelled.refundsCredits());
        assertEquals(true, rejected.refundsCredits());
        assertEquals(false, delivered.refundsCredits());
    }

    @Test
    void happenedBetween_ShouldUsePurchaseDateOnlyWhenDateExists() {
        Purchase purchase = purchaseWithStatus(EstadoCompra.ENTREGADO);
        purchase.setDate(LocalDateTime.of(2026, 6, 10, 9, 0));

        assertEquals(true, purchase.hasDate());
        assertEquals(LocalDate.of(2026, 6, 10), purchase.purchaseDate());
        assertEquals(true, purchase.happenedBetween(
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 30)
        ));
        assertEquals(false, purchase.happenedBetween(
                LocalDate.of(2026, 6, 11),
                LocalDate.of(2026, 6, 30)
        ));
    }

    @Test
    void happenedBetween_ShouldReturnFalseWhenDateDoesNotExist() {
        Purchase purchase = purchaseWithStatus(EstadoCompra.ENTREGADO);

        assertEquals(false, purchase.hasDate());
        assertEquals(false, purchase.happenedBetween(
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 30)
        ));
    }

    @Test
    void totalAmountOrZero_ShouldReturnZeroWhenTotalIsNull() {
        Purchase purchase = purchaseWithStatus(EstadoCompra.ENTREGADO);

        assertEquals(BigDecimal.ZERO, purchase.totalAmountOrZero());

        purchase.setTotalAmount(new BigDecimal("25.00"));

        assertEquals(new BigDecimal("25.00"), purchase.totalAmountOrZero());
    }

    @Test
    void hasPickupSlotDescription_ShouldRejectNullOrBlankDescriptions() {
        Purchase purchase = new Purchase();

        assertEquals(false, purchase.hasPickupSlotDescription());

        purchase.setPickupSlotDescription(" ");

        assertEquals(false, purchase.hasPickupSlotDescription());

        purchase.setPickupSlotDescription("Primer recreo");

        assertEquals(true, purchase.hasPickupSlotDescription());
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
