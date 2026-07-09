package com.recred.backend.application;

import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Purchase;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record GetHistoryByStudentResponse(
        UUID id,
        UUID studentId,
        BigDecimal totalAmount,
        String status,
        String statusLabel,
        String paymentMethod,
        LocalDateTime date,
        List<Item> items,
        String tipo,
        String withdrawalCode,
        String pickupSlotDescription,
        LocalDate pickupDate,
        LocalTime pickupSlotStartTime,
        String withdrawalStatus
) {
    public record Item(
            UUID productId,
            String nombre,
            Integer quantity,
            BigDecimal precio,
            Boolean requierePreparacion,
            Integer cupoMaximoDiario,
            UUID promocionId,
            String promocionNombre
    ) {}

    public static List<GetHistoryByStudentResponse> fromPurchases(List<Purchase> purchases) {
        return purchases.stream()
                .map(GetHistoryByStudentResponse::fromPurchase)
                .toList();
    }

    private static GetHistoryByStudentResponse fromPurchase(Purchase purchase) {
        return new GetHistoryByStudentResponse(
                purchase.getId(),
                studentIdFrom(purchase),
                purchase.getTotalAmount(),
                statusNameFrom(purchase),
                statusLabelFrom(purchase),
                paymentMethodNameFrom(purchase),
                purchase.getDate(),
                itemsFrom(purchase),
                purchaseTypeNameFrom(purchase),
                purchase.getWithdrawalCode(),
                purchase.getPickupSlotDescription(),
                purchase.getPickupDate(),
                purchase.getPickupSlotStartTime(),
                withdrawalStatusNameFrom(purchase)
        );
    }

    private static UUID studentIdFrom(Purchase purchase) {
        return purchase.getStudent() == null ? null : purchase.getStudent().getId();
    }

    private static String statusNameFrom(Purchase purchase) {
        return purchase.getStatus() == null ? null : purchase.getStatus().name();
    }

    private static String statusLabelFrom(Purchase purchase) {
        return purchase.getStatus() == null ? null : purchase.getStatus().getLabel();
    }

    private static String paymentMethodNameFrom(Purchase purchase) {
        return purchase.getPaymentMethod() == null ? null : purchase.getPaymentMethod().name();
    }

    private static String purchaseTypeNameFrom(Purchase purchase) {
        return purchase.getTipo() == null ? null : purchase.getTipo().name();
    }

    private static String withdrawalStatusNameFrom(Purchase purchase) {
        return purchase.getWithdrawalStatus() == null
                ? null
                : purchase.getWithdrawalStatus().name();
    }

    private static List<Item> itemsFrom(Purchase purchase) {
        if (purchase.getItems() == null) {
            return null;
        }

        return purchase.getItems().entrySet().stream()
                .map(GetHistoryByStudentResponse::itemFrom)
                .toList();
    }

    private static Item itemFrom(Map.Entry<Producto, Integer> entry) {
        Producto product = entry.getKey();
        return new Item(
                product.getId(),
                product.getNombre(),
                entry.getValue(),
                product.getPrecio(),
                product.getRequierePreparacion(),
                product.getCupoMaximoDiario(),
                product.getPromocionId(),
                product.getPromocionNombre()
        );
    }
}
