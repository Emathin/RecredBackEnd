package com.recred.backend.infrastructure.web;

import com.recred.backend.application.GetStudentWalletSummaryResponse;
import com.recred.backend.domain.models.WalletCategorySpending;
import java.math.BigDecimal;

public record WalletCategorySpendingResponseDTO(
        String categoria,
        BigDecimal monto,
        BigDecimal porcentaje
) {
    public static WalletCategorySpendingResponseDTO fromDomain(WalletCategorySpending spending) {
        return new WalletCategorySpendingResponseDTO(
                spending.category(),
                spending.amount(),
                spending.percentage()
        );
    }

    public static WalletCategorySpendingResponseDTO fromApplication(
            GetStudentWalletSummaryResponse.CategorySpending spending) {
        return new WalletCategorySpendingResponseDTO(
                spending.category(),
                spending.amount(),
                spending.percentage()
        );
    }
}
