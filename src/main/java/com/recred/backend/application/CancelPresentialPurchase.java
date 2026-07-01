package com.recred.backend.application;


import com.recred.backend.domain.models.Billetera;
import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.FranjaHoraria;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.TipoCompra;
import com.recred.backend.domain.models.TipoMovimientoBilletera;
import com.recred.backend.domain.repositories.FranjaHorariaRepository;
import com.recred.backend.domain.repositories.MovimientoBilleteraRepository;
import com.recred.backend.domain.repositories.PurchaseRepository;
import com.recred.backend.domain.repositories.WalletRepository;
import com.recred.backend.domain.services.InventoryAvailabilityService;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;

public class CancelPresentialPurchase {

    private final PurchaseRepository purchaseRepository;
    private final WalletRepository walletRepository;
    private final InventoryAvailabilityService inventoryAvailabilityService;
    private final FranjaHorariaRepository timeSlotRepository;
    private final MovimientoBilleteraRepository movimientoBilleteraRepository;

    public CancelPresentialPurchase(
            PurchaseRepository purchaseRepository,
            WalletRepository walletRepository,
            InventoryAvailabilityService inventoryAvailabilityService,
            FranjaHorariaRepository timeSlotRepository) {
        this(purchaseRepository, walletRepository, inventoryAvailabilityService,
                timeSlotRepository, null);
    }

    public CancelPresentialPurchase(
            PurchaseRepository purchaseRepository,
            WalletRepository walletRepository,
            InventoryAvailabilityService inventoryAvailabilityService,
            FranjaHorariaRepository timeSlotRepository,
            MovimientoBilleteraRepository movimientoBilleteraRepository) {
        this.purchaseRepository = purchaseRepository;
        this.walletRepository = walletRepository;
        this.inventoryAvailabilityService = inventoryAvailabilityService;
        this.timeSlotRepository = timeSlotRepository;
        this.movimientoBilleteraRepository = movimientoBilleteraRepository;
    }

    @Transactional
    public void execute(CancelPresentialPurchaseRequest request) {
        request.validate();

        Purchase purchase = purchaseRepository.findById(request.purchaseId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Purchase not found: " + request.purchaseId()));

        EstadoCompra previousStatus = purchase.getStatus();

        validateAdvancePurchaseCancellationWindow(purchase);

        purchase.cancel();
        purchase.requireStudentId();

        if (shouldReleaseReservation(purchase, previousStatus)) {
            inventoryAvailabilityService.releaseReservation(
                    purchase.getBuffetId(),
                    purchase.getItems(),
                    purchase.getId());
        }

        processRefund(purchase);

        purchaseRepository.save(purchase);
    }

    private void processRefund(Purchase purchase) {
        Billetera wallet = walletRepository.findByStudentId(purchase.getStudent().getId())
                .orElseThrow(() ->
                        new IllegalStateException("Student wallet not found for refund."));

        wallet.credit(purchase.getTotalAmount());
        walletRepository.save(wallet);

        registerRefundMovement(wallet, purchase);
    }

    private void validateAdvancePurchaseCancellationWindow(Purchase purchase) {
        if (purchase.getTipo() != TipoCompra.ANTICIPADA) {
            return;
        }

        FranjaHoraria franja = timeSlotRepository.findById(purchase.getPickupSlotId())
                .orElseThrow(() -> new IllegalStateException(
                        "Franja horaria no encontrada para el pedido"));

        ZoneId zoneId = ZoneId.of("America/Argentina/Buenos_Aires");
        LocalDateTime nowInZone = LocalDateTime.now(zoneId);
        
        purchase.validateCancellationWindow(franja, nowInZone);
    }

    private boolean shouldReleaseReservation(Purchase purchase, EstadoCompra previousStatus){
        return purchase.getTipo() == TipoCompra.ANTICIPADA
                && previousStatus == EstadoCompra.PENDIENTE;
    }

    private void registerRefundMovement(Billetera wallet, Purchase purchase) {
        if (movimientoBilleteraRepository == null) {
            return;
        }
        movimientoBilleteraRepository.registrarMovimientoSistema(
                wallet.getId(),
                TipoMovimientoBilletera.REEMBOLSO,
                purchase.getTotalAmount(),
                "Reembolso por cancelacion de compra");
    }
}
