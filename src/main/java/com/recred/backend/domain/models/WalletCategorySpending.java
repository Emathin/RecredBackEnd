package com.recred.backend.domain.models;

import java.math.BigDecimal;

public record WalletCategorySpending(
        String category,
        BigDecimal amount,
        BigDecimal percentage
) {
}
