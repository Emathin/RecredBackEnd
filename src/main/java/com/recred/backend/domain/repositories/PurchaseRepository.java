package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.Purchase;

public interface PurchaseRepository {
    void save(Purchase purchase);
}
