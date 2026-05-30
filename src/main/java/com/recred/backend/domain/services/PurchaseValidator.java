package com.recred.backend.domain.services;

import com.recred.backend.domain.models.Purchase;

public interface PurchaseValidator {
    void validate(Purchase purchase);
}
