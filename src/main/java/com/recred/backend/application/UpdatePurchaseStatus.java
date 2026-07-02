package com.recred.backend.application;

import com.recred.backend.domain.events.KiosqueroRealtimeEvent;
import com.recred.backend.domain.events.KiosqueroRealtimeEventPublisher;
import com.recred.backend.domain.events.KiosqueroRealtimeEventType;
import com.recred.backend.domain.exceptions.PurchaseNotFoundException;
import com.recred.backend.domain.models.Billetera;
import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.Purchase;
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
    public UpdatePurchaseStatusResponse execute(UpdatePurchaseStatusRequest request) {
        requireRequest(request);
        Purchase purchase = findPurchase(request.purchaseId());
        EstadoCompra previousStatus = purchase.getStatus();
        purchase.changeTrackingStatusTo(request.requestedStatus());
        handleStatusChangeSideEffects(purchase, previousStatus, request.requestedStatus());
        Purchase savedPurchase = purchaseRepository.save(purchase);
        publishOperationalEvent(savedPurchase, request.requestedStatus());
        return response(savedPurchase);
    }

    private void requireRequest(UpdatePurchaseStatusRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }
    }

    private UpdatePurchaseStatusResponse response(Purchase savedPurchase) {
        return new UpdatePurchaseStatusResponse(
                savedPurchase.getId(),
                savedPurchase.studentIdOrNull(),
                savedPurchase.statusName(),
                savedPurchase.statusLabel(),
                savedPurchase.getDate()
        );
    }

    private Purchase findPurchase(UUID purchaseId) {
        return purchaseRepository.findById(purchaseId)
                .orElseThrow(() -> new PurchaseNotFoundException(purchaseId));
    }

    private void handleStatusChangeSideEffects(
            Purchase purchase,
            EstadoCompra previousStatus,
            EstadoCompra requestedStatus
    ) {
        handleInventoryUpdates(purchase, previousStatus, requestedStatus);
        handleRefundIfNecessary(purchase, requestedStatus);
    }

    private void handleInventoryUpdates(
            Purchase purchase,
            EstadoCompra previousStatus,
            EstadoCompra requestedStatus
    ) {
        if (purchase.shouldConsumeReservationWhenChangingTo(requestedStatus)) {
            inventoryAvailabilityService.consumeReservation(
                    purchase.getBuffetId(),
                    purchase.getItems(),
                    purchase.getId());
        }

        if (purchase.shouldReleaseReservationWhenChangingFrom(previousStatus, requestedStatus)) {
            inventoryAvailabilityService.releaseReservation(
                    purchase.getBuffetId(),
                    purchase.getItems(),
                    purchase.getId());
        }
    }

    private void handleRefundIfNecessary(Purchase purchase, EstadoCompra requestedStatus) {
        if (purchase.shouldRefundWhenChangingTo(requestedStatus)) {
            refund(purchase);
        }
    }

    private void publishOperationalEvent(Purchase purchase, EstadoCompra requestedStatus) {
        if (!purchase.shouldPublishExpirationEventFor(requestedStatus)) {
            return;
        }

        realtimeEventPublisher.publish(KiosqueroRealtimeEvent.purchase(
                KiosqueroRealtimeEventType.ORDER_EXPIRED,
                purchase,
                "Pedido vencido o no retirado"));
    }

    private void refund(Purchase purchase) {
        Billetera wallet = walletRepository.findByStudentId(purchase.requireStudentId())
                .orElseThrow(() ->
                        new IllegalStateException("Student wallet not found for refund."));
        wallet.credit(purchase.getTotalAmount());
        walletRepository.save(wallet);
        registerRefundMovement(wallet, purchase);
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
