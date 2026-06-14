package com.recred.backend.domain.models;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record DailyBuffetReport(
        UUID buffetId,
        LocalDate date,
        int totalOrders,
        int deliveredOrders,
        int pendingOrders,
        int inPreparationOrders,
        int readyOrders,
        int expiredOrders,
        int cancelledOrders,
        int rejectedOrders,
        BigDecimal deliveredTotal,
        BigDecimal refundedCredits,
        int releasedReservations,
        List<DailyProductReport> products,
        List<DailyInventorySnapshot> inventory,
        List<DailyInventorySnapshot> soldOutProducts,
        List<DailyPaymentMethodReport> salesByPaymentMethod,
        List<DailyMovementReport> stockMovements
) {
    public DailyBuffetReport(
            UUID buffetId,
            LocalDate date,
            int totalOrders,
            int deliveredOrders,
            int pendingOrders,
            int inPreparationOrders,
            int readyOrders,
            int expiredOrders,
            int cancelledOrders,
            int rejectedOrders,
            BigDecimal deliveredTotal,
            List<DailyProductReport> products,
            List<DailyInventorySnapshot> inventory
    ) {
        this(
                buffetId,
                date,
                totalOrders,
                deliveredOrders,
                pendingOrders,
                inPreparationOrders,
                readyOrders,
                expiredOrders,
                cancelledOrders,
                rejectedOrders,
                deliveredTotal,
                BigDecimal.ZERO,
                0,
                products,
                inventory,
                List.of(),
                List.of(),
                List.of()
        );
    }
}
