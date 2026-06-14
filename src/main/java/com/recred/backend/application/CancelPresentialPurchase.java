package com.recred.backend.application;

import com.recred.backend.domain.exceptions.InvalidPurchaseStatusTransitionException;
import com.recred.backend.domain.models.Billetera;
import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.TipoCompra;
import com.recred.backend.domain.repositories.PurchaseRepository;
import com.recred.backend.domain.repositories.WalletRepository;
import com.recred.backend.domain.services.InventoryAvailabilityService;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

public class CancelPresentialPurchase {

    private final PurchaseRepository purchaseRepository;
    private final WalletRepository walletRepository;
    private final InventoryAvailabilityService inventoryAvailabilityService;

    public CancelPresentialPurchase(PurchaseRepository purchaseRepository,
                                    WalletRepository walletRepository,
                                    InventoryAvailabilityService inventoryAvailabilityService) {
        this.purchaseRepository = purchaseRepository;
        this.walletRepository = walletRepository;
        this.inventoryAvailabilityService = inventoryAvailabilityService;
    }

    @Transactional
    public void execute(UUID purchaseId) {
        Purchase purchase = purchaseRepository.findById(purchaseId)
                .orElseThrow(() -> new IllegalArgumentException
                        ("Purchase not found: " + purchaseId));

        EstadoCompra previousStatus = purchase.getStatus();

        if (previousStatus != EstadoCompra.PENDIENTE) {
            throw new InvalidPurchaseStatusTransitionException(
                    previousStatus,
                    EstadoCompra.CANCELADO
            );
        }

        purchase.cancel();

        if (purchase.getStudent() == null || purchase.getStudent().getId() == null) {
            throw new IllegalStateException("Purchase does not have an associated student.");
        }
        
        Billetera wallet = walletRepository.findByStudentId(purchase.getStudent().getId())
                .orElseThrow(() ->
                        new IllegalStateException("Student wallet not found for refund."));

        if (shouldReleaseReservation(purchase, previousStatus)) {
            inventoryAvailabilityService.releaseReservation(
                    purchase.getBuffetId(),
                    purchase.getItems(),
                    purchase.getId()
            );
        }

        wallet.credit(purchase.getTotalAmount());

        walletRepository.save(wallet);
        purchaseRepository.save(purchase);
    }

    private boolean shouldReleaseReservation(Purchase purchase, EstadoCompra previousStatus) {
        return purchase.getTipo() == TipoCompra.ANTICIPADA
                && previousStatus == EstadoCompra.PENDIENTE;
    }
}
