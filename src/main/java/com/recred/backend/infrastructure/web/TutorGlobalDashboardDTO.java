package com.recred.backend.infrastructure.web;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record TutorGlobalDashboardDTO(
    String tutorId,
    BigDecimal totalBalance,
    BigDecimal totalDebt,
    String dashboardConfig,
    List<ChildDashboardDTO> children,
    String plan
) {
    public record ChildDashboardDTO(
        UUID studentId,
        String studentName,
        BigDecimal balance,
        BigDecimal debt,
        String urlFotoPerfil,
        String spendingPredictionMessage,
        BudgetSummaryDTO budget,
        HealthSummaryDTO health,
        List<ScheduledPickupDTO> todayPickups,
        List<TransactionSummaryDTO> recentTransactions,
        SmartActionDTO smartAction
    ) {}

    public record BudgetSummaryDTO(
        String period,
        BigDecimal spent,
        BigDecimal limit
    ) {}

    public record HealthSummaryDTO(
        String rewardLevel,
        Integer rewardPoints,
        Integer pointsToNextLevel,
        String weeklyHealthSummary
    ) {}

    public record ScheduledPickupDTO(
        String id,
        String menuName,
        String pickupTime
    ) {}

    public record TransactionSummaryDTO(
        String id,
        String name,
        LocalDateTime date,
        BigDecimal amount,
        String icon
    ) {}

    public record SmartActionDTO(
        String title,
        String description,
        String actionType,
        String actionPayload
    ) {}
}
