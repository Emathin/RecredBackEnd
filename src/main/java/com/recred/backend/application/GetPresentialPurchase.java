package com.recred.backend.application;

import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.repositories.PurchaseRepository;

import java.util.Optional;
import java.util.UUID;

public class GetPresentialPurchase {

    private final PurchaseRepository purchaseRepository;

    public GetPresentialPurchase(PurchaseRepository purchaseRepository) {
        this.purchaseRepository = purchaseRepository;
    }

    public Optional<Purchase> execute(UUID purchaseId) {
        return purchaseRepository.findById(purchaseId);
    }
}
