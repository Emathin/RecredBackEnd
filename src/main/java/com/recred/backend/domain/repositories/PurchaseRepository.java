package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.Purchase;

import java.util.Optional;
import java.util.UUID;

public interface PurchaseRepository {
    void save(Purchase purchase);
    Optional<Purchase> findById(UUID id);
}
