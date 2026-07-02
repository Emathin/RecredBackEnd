package com.recred.backend.application;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record GetTutorGlobalDashboardResponse(
        String tutorId,
        BigDecimal totalBalance,
        BigDecimal totalDebt,
        String dashboardConfig,
        List<ChildDashboardResponse> childrenDashboards,
        String planStr
) {
    public record ChildDashboardResponse(
            UUID id,
            String nombre,
            BigDecimal saldoCreditos,
            BigDecimal saldoDeuda,
            String urlFotoPerfil,
            String predictionMessage,
            BudgetSummaryResponse budget,
            HealthSummaryResponse health,
            List<ScheduledPickupResponse> todayPickups,
            List<TransactionSummaryResponse> recentTransactions,
            SmartActionResponse smartAction
    ) {}

    public record BudgetSummaryResponse(
            String periodName,
            BigDecimal spent,
            BigDecimal limit
    ) {}

    public record HealthSummaryResponse(
            String rewardLevel,
            int rewardPoints,
            int pointsToNextLevel,
            String weeklyHealthSummary
    ) {}

    public record ScheduledPickupResponse(
            String purchaseId,
            String title,
            String slotDescription
    ) {}

    public record TransactionSummaryResponse(
            String id,
            String title,
            LocalDateTime date,
            BigDecimal amount,
            String icon
    ) {}

    public record SmartActionResponse(
            String title,
            String description,
            String actionType,
            String actionValue
    ) {}
}
