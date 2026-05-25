package com.recred.backend.application;

import com.recred.backend.domain.models.Billetera;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.repositories.PurchaseRepository;
import com.recred.backend.domain.repositories.WalletRepository;

import java.util.UUID;

public class CancelPresentialPurchase {

    private final PurchaseRepository purchaseRepository;
    private final WalletRepository walletRepository;

    public CancelPresentialPurchase(PurchaseRepository purchaseRepository, WalletRepository walletRepository) {
        this.purchaseRepository = purchaseRepository;
        this.walletRepository = walletRepository;
    }

    public void execute(UUID purchaseId) {
        Purchase purchase = purchaseRepository.findById(purchaseId)
                .orElseThrow(() -> new IllegalArgumentException("Purchase not found: " + purchaseId));


        purchase.cancel();

        if (purchase.getStudent() == null || purchase.getStudent().getId() == null) {
            throw new IllegalStateException("Purchase does not have an associated student.");
        }
        
        Billetera wallet = walletRepository.findByStudentId(purchase.getStudent().getId())
                .orElseThrow(() -> new IllegalStateException("Student wallet not found for refund."));

        wallet.credit(purchase.getTotalAmount());

        walletRepository.save(wallet);
        purchaseRepository.save(purchase);
    }
}
