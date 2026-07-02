package com.recred.backend.application;

import com.recred.backend.domain.models.DailyInventorySnapshot;
import com.recred.backend.domain.models.EstadoInventario;
import com.recred.backend.domain.models.MedioPago;
import com.recred.backend.domain.models.TipoManejoInventario;
import com.recred.backend.domain.models.TipoMovimientoStock;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record GenerateDailyBuffetReportResponse(
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
        List<DailyProductReportResponse> products,
        List<DailyInventorySnapshotResponse> inventory,
        List<DailyInventorySnapshotResponse> soldOutProducts,
        List<DailyPaymentMethodReportResponse> salesByPaymentMethod,
        List<DailyMovementReportResponse> stockMovements
) {
    List<DailyInventorySnapshot> inventorySnapshotDomain() {
        return inventory.stream()
                .map(DailyInventorySnapshotResponse::toDomain)
                .toList();
    }

    public record DailyProductReportResponse(
            UUID productId,
            String productName,
            int quantity,
            BigDecimal total
    ) {
    }

    public record DailyInventorySnapshotResponse(
            UUID productId,
            String productName,
            Integer stockActual,
            Integer stockReservado,
            Integer stockDisponible,
            Integer stockMinimo,
            Integer cupoMaximoDiario,
            Integer cupoDisponibleDia,
            EstadoInventario estadoInventario,
            TipoManejoInventario tipoManejoInventario
    ) {
        static DailyInventorySnapshotResponse fromDomain(DailyInventorySnapshot snapshot) {
            return new DailyInventorySnapshotResponse(
                    snapshot.productId(),
                    snapshot.productName(),
                    snapshot.stockActual(),
                    snapshot.stockReservado(),
                    snapshot.stockDisponible(),
                    snapshot.stockMinimo(),
                    snapshot.cupoMaximoDiario(),
                    snapshot.cupoDisponibleDia(),
                    snapshot.estadoInventario(),
                    snapshot.tipoManejoInventario()
            );
        }

        DailyInventorySnapshot toDomain() {
            return new DailyInventorySnapshot(
                    productId,
                    productName,
                    stockActual,
                    stockReservado,
                    stockDisponible,
                    stockMinimo,
                    cupoMaximoDiario,
                    cupoDisponibleDia,
                    estadoInventario,
                    tipoManejoInventario
            );
        }
    }

    public record DailyPaymentMethodReportResponse(
            MedioPago paymentMethod,
            int orders,
            BigDecimal total
    ) {
    }

    public record DailyMovementReportResponse(
            TipoMovimientoStock movementType,
            int quantity
    ) {
    }
}
