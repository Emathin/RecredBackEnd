package com.recred.backend.infrastructure.web;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.BuyerType;
import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.TipoCompra;
import com.recred.backend.domain.models.Usuario;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record KiosqueroOrderResponse(
        UUID id,
        UUID buffetId,
        TipoCompra tipo,
        EstadoCompra status,
        String statusLabel,
        BigDecimal totalAmount,
        String paymentMethod,
        LocalDateTime date,
        String withdrawalCode,
        LocalDate pickupDate,
        PickupSlot pickupSlot,
        Student student,
        Buyer buyer,
        List<Item> items
) {
    public static KiosqueroOrderResponse fromDomain(Purchase purchase) {
        return fromDomain(purchase, Map.of());
    }

    public static KiosqueroOrderResponse fromDomain(
            Purchase purchase,
            Map<UUID, Producto> inventoryProducts
    ) {
        return new KiosqueroOrderResponse(
                purchase.getId(),
                purchase.getBuffetId(),
                purchase.getTipo(),
                purchase.getStatus(),
                purchase.getStatus() == null ? null : purchase.getStatus().getLabel(),
                purchase.getTotalAmount(),
                purchase.getPaymentMethod() == null ? null : purchase.getPaymentMethod().name(),
                purchase.getDate(),
                purchase.getWithdrawalCode(),
                purchase.getPickupDate(),
                pickupSlot(purchase),
                student(purchase.getStudent()),
                buyer(purchase),
                items(purchase.getItems(), inventoryProducts)
        );
    }

    private static PickupSlot pickupSlot(Purchase purchase) {
        if (purchase.getPickupSlotId() == null) {
            return null;
        }

        return new PickupSlot(
                purchase.getPickupSlotId(),
                purchase.getPickupSlotDescription(),
                purchase.getPickupSlotStart(),
                purchase.getPickupSlotEnd()
        );
    }

    private static Student student(Alumno student) {
        if (student == null) {
            return null;
        }

        Usuario user = student.getUsuario();
        return new Student(
                student.getId(),
                user == null ? null : user.getNombre(),
                user == null ? null : user.getApellido(),
                student.getGrado() == null ? null : student.getGrado().getAnio(),
                student.getGrado() == null ? null : student.getGrado().getDivision()
        );
    }

    private static Buyer buyer(Purchase purchase) {
        BuyerType buyerType = purchase.getBuyerType();
        return new Buyer(purchase.getBuyerId(), buyerType == null ? null : buyerType.name());
    }

    private static List<Item> items(
            Map<Producto, Integer> items,
            Map<UUID, Producto> inventoryProducts
    ) {
        if (items == null) {
            return List.of();
        }

        return items.entrySet().stream()
                .map(entry -> item(product(entry.getKey(), inventoryProducts), entry.getValue()))
                .toList();
    }

    private static Producto product(Producto product, Map<UUID, Producto> inventoryProducts) {
        if (product == null || inventoryProducts == null) {
            return product;
        }

        return inventoryProducts.getOrDefault(product.getId(), product);
    }

    private static Item item(Producto product, Integer quantity) {
        BigDecimal unitPrice = product.getPrecio() == null
                ? BigDecimal.ZERO
                : product.getPrecio();
        return new Item(
                product.getId(),
                product.getNombre(),
                quantity,
                unitPrice,
                unitPrice.multiply(BigDecimal.valueOf(quantity)),
                product.getRequierePreparacion(),
                product.getCupoMaximoDiario()
        );
    }

    public record Student(
            UUID id,
            String firstName,
            String lastName,
            String gradeYear,
            String gradeDivision
    ) {
    }

    public record Buyer(UUID id, String type) {
    }

    public record PickupSlot(
            UUID id,
            String description,
            LocalTime start,
            LocalTime end
    ) {
    }

    public record Item(
            UUID productId,
            String productName,
            Integer quantity,
            BigDecimal unitPrice,
            BigDecimal subtotal,
            Boolean requierePreparacion,
            Integer cupoMaximoDiario
    ) {
    }
}
