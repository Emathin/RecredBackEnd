package com.recred.backend.application;

import com.recred.backend.domain.models.Billetera;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.repositories.PurchaseRepository;
import com.recred.backend.domain.repositories.WalletRepository;

import java.util.UUID;

public class CancelPurchase {

    private final PurchaseRepository purchaseRepository;
    private final WalletRepository walletRepository;

    public CancelPurchase(PurchaseRepository purchaseRepository, WalletRepository walletRepository) {
        this.purchaseRepository = purchaseRepository;
        this.walletRepository = walletRepository;
    }

    public void execute(UUID purchaseId) {
        Purchase purchase = purchaseRepository.findById(purchaseId)
                .orElseThrow(() -> new IllegalArgumentException("Purchase not found: " + purchaseId));

        // 1. Cancel the purchase (changes status to CANCELADO)
        purchase.cancel();

        // 2. Fetch the wallet of the student who made the purchase
        // Note: In our domain model mapping, we need to ensure the student ID is available on the purchase.
        if (purchase.getStudent() == null || purchase.getStudent().getId() == null) {
            throw new IllegalStateException("Purchase does not have an associated student.");
        }
        
        Billetera wallet = walletRepository.findByStudentId(purchase.getStudent().getId())
                .orElseThrow(() -> new IllegalStateException("Student wallet not found for refund."));

        // 3. Refund the total amount back to the wallet
        wallet.credit(purchase.getTotalAmount());

        // 4. Save the changes
        walletRepository.save(wallet);
        purchaseRepository.save(purchase);
    }
}
