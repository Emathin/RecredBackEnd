package com.recred.backend.infrastructure.web;

import com.recred.backend.domain.models.WalletCategorySpending;
import java.math.BigDecimal;

public record WalletCategorySpendingResponse(
        String categoria,
        BigDecimal monto,
        BigDecimal porcentaje
) {
    public static WalletCategorySpendingResponse fromDomain(WalletCategorySpending spending) {
        return new WalletCategorySpendingResponse(
                spending.category(),
                spending.amount(),
                spending.percentage()
        );
    }
}
