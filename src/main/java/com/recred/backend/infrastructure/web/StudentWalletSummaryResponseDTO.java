package com.recred.backend.infrastructure.web;

import com.recred.backend.application.GetStudentWalletSummaryResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record StudentWalletSummaryResponseDTO(
        UUID alumnoId,
        BigDecimal saldoActual,
        WalletPeriodResponseDTO periodo,
        BigDecimal montoIngresado,
        BigDecimal montoGastado,
        BigDecimal balancePeriodo,
        int cantidadCompras,
        List<WalletCategorySpendingResponseDTO> gastoPorCategoria,
        List<WalletHealthSpendingResponseDTO> gastoPorClasificacionSalud,
        List<WalletMovementResponseDTO> movimientos
) {
    public static StudentWalletSummaryResponseDTO fromApplication(
            GetStudentWalletSummaryResponse summary) {
        return new StudentWalletSummaryResponseDTO(
                summary.studentId(),
                summary.currentBalance(),
                new WalletPeriodResponseDTO(summary.from(), summary.to()),
                summary.incomeAmount(),
                summary.spentAmount(),
                summary.periodBalance(),
                summary.purchaseCount(),
                summary.categorySpending().stream()
                        .map(WalletCategorySpendingResponseDTO::fromApplication)
                        .toList(),
                summary.healthSpending().stream()
                        .map(WalletHealthSpendingResponseDTO::fromApplication)
                        .toList(),
                summary.movements().stream()
                        .map(WalletMovementResponseDTO::fromApplication)
                        .toList()
        );
    }
}
