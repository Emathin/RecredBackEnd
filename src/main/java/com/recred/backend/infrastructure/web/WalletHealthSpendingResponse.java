package com.recred.backend.infrastructure.web;

import com.recred.backend.domain.models.WalletHealthSpending;
import java.math.BigDecimal;

public record WalletHealthSpendingResponse(
        String clasificacion,
        BigDecimal monto,
        BigDecimal porcentaje
) {
    public static WalletHealthSpendingResponse fromDomain(WalletHealthSpending spending) {
        return new WalletHealthSpendingResponse(
                spending.classification(),
                spending.amount(),
                spending.percentage()
        );
    }
}
