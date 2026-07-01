package com.recred.backend.application;

import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Purchase;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CreateAssistantAdvancePurchaseResponse(
        UUID purchaseId,
        UUID studentId,
        UUID buffetId,
        EstadoCompra status,
        String withdrawalCode,
        BigDecimal totalAmount,
        LocalDate pickupDate,
        UUID pickupSlotId,
        LocalDateTime date,
        List<Item> items
) {
    static CreateAssistantAdvancePurchaseResponse fromDomain(Purchase purchase) {
        return new CreateAssistantAdvancePurchaseResponse(
                purchase.getId(),
                purchase.studentIdOrNull(),
                purchase.getBuffetId(),
                purchase.getStatus(),
                purchase.getWithdrawalCode(),
                purchase.getTotalAmount(),
                purchase.getPickupDate(),
                purchase.getPickupSlotId(),
                purchase.getDate(),
                itemsFrom(purchase)
        );
    }

    private static List<Item> itemsFrom(Purchase purchase) {
        if (purchase.getItems() == null) {
            return List.of();
        }

        return purchase.getItems()
                .entrySet()
                .stream()
                .filter(entry -> entry.getKey() != null && entry.getValue() != null)
                .map(entry -> fromItem(entry.getKey(), entry.getValue()))
                .toList();
    }

    private static Item fromItem(Producto product, Integer quantity) {
        return new Item(
                product.getId(),
                product.getNombre(),
                quantity,
                product.getPrecio()
        );
    }

    public record Item(
            UUID productId,
            String name,
            int quantity,
            BigDecimal price
    ) {}
}
