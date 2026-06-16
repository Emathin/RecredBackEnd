package com.recred.backend.domain.models;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record StudentWalletSummary(
        UUID studentId,
        BigDecimal currentBalance,
        LocalDate from,
        LocalDate to,
        BigDecimal incomeAmount,
        BigDecimal spentAmount,
        BigDecimal periodBalance,
        int purchaseCount,
        List<WalletCategorySpending> categorySpending,
        List<WalletHealthSpending> healthSpending,
        List<WalletMovement> movements
) {
}
