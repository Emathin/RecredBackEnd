package com.recred.backend.application;

import com.recred.backend.domain.events.KiosqueroRealtimeEvent;
import com.recred.backend.domain.events.KiosqueroRealtimeEventPublisher;
import com.recred.backend.domain.events.KiosqueroRealtimeEventType;
import com.recred.backend.domain.exceptions.InvalidPurchaseStatusTransitionException;
import com.recred.backend.domain.exceptions.PurchaseNotFoundException;
import com.recred.backend.domain.models.Billetera;
import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.PurchaseStatus;
import com.recred.backend.domain.models.TipoCompra;
import com.recred.backend.domain.models.TipoMovimientoBilletera;
import com.recred.backend.domain.repositories.MovimientoBilleteraRepository;
import com.recred.backend.domain.repositories.PurchaseRepository;
import com.recred.backend.domain.repositories.WalletRepository;
import com.recred.backend.domain.services.InventoryAvailabilityService;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

public class UpdatePurchaseStatus {

    private final PurchaseRepository purchaseRepository;
    private final InventoryAvailabilityService inventoryAvailabilityService;
    private final WalletRepository walletRepository;
    private final KiosqueroRealtimeEventPublisher realtimeEventPublisher;
    private final MovimientoBilleteraRepository movimientoBilleteraRepository;

    public UpdatePurchaseStatus(
            PurchaseRepository purchaseRepository,
            InventoryAvailabilityService inventoryAvailabilityService,
            WalletRepository walletRepository) {
        this(purchaseRepository, inventoryAvailabilityService, walletRepository, event -> {
        });
    }

    public UpdatePurchaseStatus(
            PurchaseRepository purchaseRepository,
            InventoryAvailabilityService inventoryAvailabilityService,
            WalletRepository walletRepository,
            KiosqueroRealtimeEventPublisher realtimeEventPublisher) {
        this(purchaseRepository, inventoryAvailabilityService, walletRepository,
                realtimeEventPublisher, null);
    }

    public UpdatePurchaseStatus(
            PurchaseRepository purchaseRepository,
            InventoryAvailabilityService inventoryAvailabilityService,
            WalletRepository walletRepository,
            KiosqueroRealtimeEventPublisher realtimeEventPublisher,
            MovimientoBilleteraRepository movimientoBilleteraRepository) {
        this.purchaseRepository = purchaseRepository;
        this.inventoryAvailabilityService = inventoryAvailabilityService;
        this.walletRepository = walletRepository;
        this.realtimeEventPublisher = realtimeEventPublisher;
        this.movimientoBilleteraRepository = movimientoBilleteraRepository;
    }

    @Transactional
    public PurchaseStatus execute(UUID purchaseId, EstadoCompra requestedStatus) {
        Purchase purchase = purchaseRepository.findById(purchaseId)
                .orElseThrow(() -> new PurchaseNotFoundException(purchaseId));
        EstadoCompra previousStatus = purchase.getStatus();

        if (requestedStatus == EstadoCompra.CANCELADO) {
            InvalidPurchaseStatusTransitionException exception = 
                new InvalidPurchaseStatusTransitionException(
                    purchase.getStatus(),
                    requestedStatus);
            throw exception;
        }

        purchase.changeStatusTo(requestedStatus);

        if (shouldConsumeReservation(purchase, requestedStatus)) {
            inventoryAvailabilityService.consumeReservation(
                    purchase.getBuffetId(),
                    purchase.getItems(),
                    purchase.getId());
        }

        if (shouldReleaseReservation(purchase, previousStatus, requestedStatus)) {
            inventoryAvailabilityService.releaseReservation(
                    purchase.getBuffetId(),
                    purchase.getItems(),
                    purchase.getId());
        }

        if (shouldRefund(purchase, requestedStatus)) {
            refund(purchase);
        }

        Purchase savedPurchase = purchaseRepository.save(purchase);
        publishOperationalEvent(savedPurchase, requestedStatus);

        return PurchaseStatus.from(savedPurchase);
    }

    private void publishOperationalEvent(Purchase purchase, EstadoCompra requestedStatus) {
        if (requestedStatus != EstadoCompra.VENCIDO) {
            return;
        }

        realtimeEventPublisher.publish(KiosqueroRealtimeEvent.purchase(
                KiosqueroRealtimeEventType.ORDER_EXPIRED,
                purchase,
                "Pedido vencido o no retirado"));
    }

    private boolean shouldConsumeReservation(Purchase purchase, EstadoCompra requestedStatus) {
        return purchase.getTipo() == TipoCompra.ANTICIPADA
                && requestedStatus == EstadoCompra.ENTREGADO;
    }

    private boolean shouldReleaseReservation(
            Purchase purchase,
            EstadoCompra previousStatus,
            EstadoCompra requestedStatus) {
        return purchase.getTipo() == TipoCompra.ANTICIPADA
                && hasActiveReservation(previousStatus)
                && (requestedStatus == EstadoCompra.RECHAZADO
                        || requestedStatus == EstadoCompra.VENCIDO);
    }

    private boolean shouldRefund(Purchase purchase, EstadoCompra requestedStatus) {
        return purchase.getTipo() == TipoCompra.ANTICIPADA
                && requestedStatus == EstadoCompra.RECHAZADO;
    }

    private void refund(Purchase purchase) {
        if (purchase.getStudent() == null || purchase.getStudent().getId() == null) {
            throw new IllegalStateException("Purchase does not have an associated student.");
        }

        Billetera wallet = walletRepository.findByStudentId(purchase.getStudent().getId())
                .orElseThrow(() -> 
                             new IllegalStateException("Student wallet not found for refund."));
        wallet.credit(purchase.getTotalAmount());
        walletRepository.save(wallet);
        registerRefundMovement(wallet, purchase);
    }

    private boolean hasActiveReservation(EstadoCompra status) {
        return status == EstadoCompra.PENDIENTE
                || status == EstadoCompra.EN_PREPARACION
                || status == EstadoCompra.LISTO;
    }

    private void registerRefundMovement(Billetera wallet, Purchase purchase) {
        if (movimientoBilleteraRepository == null) {
            return;
        }
        movimientoBilleteraRepository.registrarMovimientoSistema(
                wallet.getId(),
                TipoMovimientoBilletera.REEMBOLSO,
                purchase.getTotalAmount(),
                "Reembolso por rechazo de compra");
    }
}
