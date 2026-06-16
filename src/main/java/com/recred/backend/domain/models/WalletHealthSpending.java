package com.recred.backend.domain.models;

import java.math.BigDecimal;

public record WalletHealthSpending(
        String classification,
        BigDecimal amount,
        BigDecimal percentage
) {
}
