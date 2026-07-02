package com.recred.backend.application;

import java.math.BigDecimal;
import java.util.UUID;

public record ProcessPresentialPurchaseResponse(
        UUID purchaseId,
        BigDecimal totalAmount,
        String status
) {}
