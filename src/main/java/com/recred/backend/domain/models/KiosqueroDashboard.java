package com.recred.backend.domain.models;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record KiosqueroDashboard(
        UUID buffetId,
        LocalDate date,
        DashboardSummary summary,
        DashboardActivity activity,
        DashboardProducts products,
        DashboardAlerts alerts,
        DashboardTrends trends
) {
    public record DashboardSummary(
            BigDecimal totalSold,
            int totalOrders,
            int deliveredOrders,
            BigDecimal averageTicket,
            int pendingOrders,
            int soldOutProducts
    ) {
    }

    public record DashboardActivity(
            List<DashboardTimeSlotSales> salesByTimeSlot,
            List<DashboardCategorySales> salesByCategory,
            List<DashboardStatusOrders> ordersByStatus,
            List<DashboardTypeOrders> ordersByPurchaseType
    ) {
    }

    public record DashboardProducts(
            List<DashboardProductMetric> topSoldProducts,
            List<DashboardProductMetric> mostReservedProducts,
            List<DashboardInventoryAttention> productsNeedingRestock,
            List<DashboardInventoryAttention> soldOutProducts
    ) {
    }

    public record DashboardAlerts(
            int expiredOrders,
            int releasedReservations,
            BigDecimal refundedCredits,
            int soldOutEvents,
            int pendingOrders,
            int readyOrders,
            List<DashboardAlert> items
    ) {
    }

    public record DashboardTrends(
            List<DashboardTrendPoint> lastSevenDays,
            List<DashboardTrendPoint> salesByDay
    ) {
        public DashboardTrends(List<DashboardTrendPoint> lastSevenDays) {
            this(lastSevenDays, lastSevenDays);
        }
    }

    public record DashboardTimeSlotSales(
            UUID pickupSlotId,
            String timeSlot,
            LocalTime pickupSlotStartTime,
            LocalTime pickupSlotEndTime,
            int orders,
            BigDecimal totalSold
    ) {
    }

    public record DashboardCategorySales(
            UUID categoryId,
            String categoryName,
            int quantity,
            BigDecimal total
    ) {
    }

    public record DashboardStatusOrders(
            EstadoCompra status,
            String label,
            int orders
    ) {
    }

    public record DashboardTypeOrders(
            TipoCompra type,
            int orders
    ) {
    }

    public record DashboardProductMetric(
            UUID productId,
            String productName,
            int quantity,
            BigDecimal total
    ) {
    }

    public record DashboardInventoryAttention(
            UUID productId,
            String productName,
            Integer stockDisponible,
            Integer stockMinimo,
            String estadoInventario
    ) {
    }

    public record DashboardAlert(
            String type,
            String label,
            int quantity,
            BigDecimal amount
    ) {
    }

    public record DashboardTrendPoint(
            LocalDate date,
            BigDecimal totalSold,
            int totalOrders,
            int deliveredOrders,
            int createdOrders,
            int pendingOrders,
            int inPreparationOrders,
            int readyOrders,
            int cancelledOrders,
            int rejectedOrders,
            int expiredOrders
    ) {
        public DashboardTrendPoint(
                LocalDate date,
                BigDecimal totalSold,
                int totalOrders,
                int deliveredOrders
        ) {
            this(
                    date,
                    totalSold,
                    totalOrders,
                    deliveredOrders,
                    totalOrders,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0
            );
        }
    }
}
