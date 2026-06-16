package com.recred.backend.infrastructure.web;

import com.recred.backend.domain.models.StudentWalletSummary;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record StudentWalletSummaryResponse(
        UUID alumnoId,
        BigDecimal saldoActual,
        WalletPeriodResponse periodo,
        BigDecimal montoIngresado,
        BigDecimal montoGastado,
        BigDecimal balancePeriodo,
        int cantidadCompras,
        List<WalletCategorySpendingResponse> gastoPorCategoria,
        List<WalletHealthSpendingResponse> gastoPorClasificacionSalud,
        List<WalletMovementResponse> movimientos
) {
    public static StudentWalletSummaryResponse fromDomain(StudentWalletSummary summary) {
        return new StudentWalletSummaryResponse(
                summary.studentId(),
                summary.currentBalance(),
                new WalletPeriodResponse(summary.from(), summary.to()),
                summary.incomeAmount(),
                summary.spentAmount(),
                summary.periodBalance(),
                summary.purchaseCount(),
                summary.categorySpending().stream()
                        .map(WalletCategorySpendingResponse::fromDomain)
                        .toList(),
                summary.healthSpending().stream()
                        .map(WalletHealthSpendingResponse::fromDomain)
                        .toList(),
                summary.movements().stream()
                        .map(WalletMovementResponse::fromDomain)
                        .toList()
        );
    }
}
