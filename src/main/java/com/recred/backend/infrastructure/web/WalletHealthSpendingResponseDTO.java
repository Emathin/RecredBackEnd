package com.recred.backend.infrastructure.web;

import com.recred.backend.application.GetStudentWalletSummaryResponse;
import com.recred.backend.domain.models.WalletHealthSpending;
import java.math.BigDecimal;

public record WalletHealthSpendingResponseDTO(
        String clasificacion,
        BigDecimal monto,
        BigDecimal porcentaje
) {
    public static WalletHealthSpendingResponseDTO fromDomain(WalletHealthSpending spending) {
        return new WalletHealthSpendingResponseDTO(
                spending.classification(),
                spending.amount(),
                spending.percentage()
        );
    }

    public static WalletHealthSpendingResponseDTO fromApplication(
            GetStudentWalletSummaryResponse.HealthSpending spending) {
        return new WalletHealthSpendingResponseDTO(
                spending.classification(),
                spending.amount(),
                spending.percentage()
        );
    }
}
