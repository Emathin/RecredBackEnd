package com.recred.backend.application;

import com.recred.backend.domain.models.StudentWalletSummary;
import com.recred.backend.domain.models.WalletCategorySpending;
import com.recred.backend.domain.models.WalletHealthSpending;
import com.recred.backend.domain.models.WalletMovement;
import com.recred.backend.domain.models.WalletMovementDirection;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record GetStudentWalletSummaryResponse(
        UUID studentId,
        BigDecimal currentBalance,
        LocalDate from,
        LocalDate to,
        BigDecimal incomeAmount,
        BigDecimal spentAmount,
        BigDecimal periodBalance,
        int purchaseCount,
        List<CategorySpending> categorySpending,
        List<HealthSpending> healthSpending,
        List<Movement> movements
) {

    public static GetStudentWalletSummaryResponse fromDomain(StudentWalletSummary summary) {
        return new GetStudentWalletSummaryResponse(
                summary.studentId(),
                summary.currentBalance(),
                summary.from(),
                summary.to(),
                summary.incomeAmount(),
                summary.spentAmount(),
                summary.periodBalance(),
                summary.purchaseCount(),
                summary.categorySpending().stream()
                        .map(GetStudentWalletSummaryResponse::toCategorySpending)
                        .toList(),
                summary.healthSpending().stream()
                        .map(GetStudentWalletSummaryResponse::toHealthSpending)
                        .toList(),
                summary.movements().stream()
                        .map(GetStudentWalletSummaryResponse::toMovement)
                        .toList()
        );
    }

    private static CategorySpending toCategorySpending(WalletCategorySpending spending) {
        return new CategorySpending(
                spending.category(),
                spending.amount(),
                spending.percentage()
        );
    }

    private static HealthSpending toHealthSpending(WalletHealthSpending spending) {
        return new HealthSpending(
                spending.classification(),
                spending.amount(),
                spending.percentage()
        );
    }

    private static Movement toMovement(WalletMovement movement) {
        return new Movement(
                movement.id(),
                movement.dateTime(),
                movement.type().name(),
                movement.description(),
                movement.amount(),
                movement.direction()
        );
    }

    public record CategorySpending(
            String category,
            BigDecimal amount,
            BigDecimal percentage
    ) {
    }

    public record HealthSpending(
            String classification,
            BigDecimal amount,
            BigDecimal percentage
    ) {
    }

    public record Movement(
            UUID id,
            LocalDateTime dateTime,
            String type,
            String description,
            BigDecimal amount,
            WalletMovementDirection direction
    ) {
    }
}
