package com.recred.backend.infrastructure.web;

import com.recred.backend.application.ListInventoryProductsResponse.InventoryProductResponse;
import com.recred.backend.application.ListKiosqueroOrdersResponse.BuyerResponse;
import com.recred.backend.application.ListKiosqueroOrdersResponse.ItemResponse;
import com.recred.backend.application.ListKiosqueroOrdersResponse.OrderResponse;
import com.recred.backend.application.ListKiosqueroOrdersResponse.PickupSlotResponse;
import com.recred.backend.application.ListKiosqueroOrdersResponse.StudentResponse;
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
        List<Item> items,
        String cancelledByRole
) {
    public static KiosqueroOrderResponse fromDomain(Purchase purchase) {
        return fromDomain(purchase, Map.of());
    }

    public static KiosqueroOrderResponse fromApplication(OrderResponse order) {
        return fromApplication(order, Map.of());
    }

    public static KiosqueroOrderResponse fromApplication(
            OrderResponse order,
            Map<UUID, InventoryProductResponse> inventoryProducts
    ) {
        return new KiosqueroOrderResponse(
                order.id(),
                order.buffetId(),
                enumValue(TipoCompra.class, order.type()),
                enumValue(EstadoCompra.class, order.status()),
                order.statusLabel(),
                order.totalAmount(),
                order.paymentMethod(),
                order.date(),
                order.withdrawalCode(),
                order.pickupDate(),
                pickupSlot(order.pickupSlot()),
                student(order.student()),
                buyer(order.buyer()),
                items(order.items(), inventoryProducts),
                order.cancelledByRole()
        );
    }

    public static KiosqueroOrderResponse fromDomain(
            Purchase purchase,
            Map<UUID, InventoryProductResponse> inventoryProducts
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
                items(purchase.getItems(), inventoryProducts),
                cancelledByRoleNameFrom(purchase)
        );
    }

    private static String cancelledByRoleNameFrom(Purchase purchase) {
        return purchase.getCancelledByRole() == null ? null : purchase.getCancelledByRole().name();
    }

    private static <T extends Enum<T>> T enumValue(Class<T> enumType, String value) {
        return value == null ? null : Enum.valueOf(enumType, value);
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

    private static PickupSlot pickupSlot(PickupSlotResponse pickupSlot) {
        if (pickupSlot == null) {
            return null;
        }

        return new PickupSlot(
                pickupSlot.id(),
                pickupSlot.description(),
                pickupSlot.start(),
                pickupSlot.end()
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

    private static Student student(StudentResponse student) {
        if (student == null) {
            return null;
        }

        return new Student(
                student.id(),
                student.firstName(),
                student.lastName(),
                student.gradeYear(),
                student.gradeDivision()
        );
    }

    private static Buyer buyer(Purchase purchase) {
        BuyerType buyerType = purchase.getBuyerType();
        return new Buyer(purchase.getBuyerId(), buyerType == null ? null : buyerType.name());
    }

    private static Buyer buyer(BuyerResponse buyer) {
        if (buyer == null) {
            return null;
        }

        return new Buyer(buyer.id(), buyer.type());
    }

    private static List<Item> items(
            Map<Producto, Integer> items,
            Map<UUID, InventoryProductResponse> inventoryProducts
    ) {
        if (items == null) {
            return List.of();
        }

        return items.entrySet().stream()
                .map(entry -> item(product(entry.getKey(), inventoryProducts), entry.getValue()))
                .toList();
    }

    private static List<Item> items(
            List<ItemResponse> items,
            Map<UUID, InventoryProductResponse> inventoryProducts
    ) {
        if (items == null) {
            return List.of();
        }

        return items.stream()
                .map(item -> item(product(item, inventoryProducts), item.quantity()))
                .toList();
    }

    private static OrderProduct product(
            Producto product,
            Map<UUID, InventoryProductResponse> inventoryProducts
    ) {
        if (product == null || inventoryProducts == null) {
            return OrderProduct.fromDomain(product);
        }

        InventoryProductResponse inventoryProduct = inventoryProducts.get(product.getId());
        return inventoryProduct == null
                ? OrderProduct.fromDomain(product)
                : OrderProduct.fromResponse(inventoryProduct);
    }

    private static OrderProduct product(
            ItemResponse product,
            Map<UUID, InventoryProductResponse> inventoryProducts
    ) {
        if (inventoryProducts == null) {
            return OrderProduct.fromResponse(product);
        }

        InventoryProductResponse inventoryProduct = inventoryProducts.get(product.productId());
        return inventoryProduct == null
                ? OrderProduct.fromResponse(product)
                : OrderProduct.fromResponse(inventoryProduct);
    }

    private static Item item(OrderProduct product, Integer quantity) {
        BigDecimal unitPrice = product.price() == null
                ? BigDecimal.ZERO
                : product.price();
        return new Item(
                product.id(),
                product.name(),
                quantity,
                unitPrice,
                unitPrice.multiply(BigDecimal.valueOf(quantity)),
                product.requiresPreparation(),
                product.dailyCapacity()
        );
    }

    private record OrderProduct(
            UUID id,
            String name,
            BigDecimal price,
            Boolean requiresPreparation,
            Integer dailyCapacity
    ) {
        private static OrderProduct fromDomain(Producto product) {
            return new OrderProduct(
                    product.getId(),
                    product.getNombre(),
                    product.getPrecio(),
                    product.getRequierePreparacion(),
                    product.getCupoMaximoDiario()
            );
        }

        private static OrderProduct fromResponse(InventoryProductResponse product) {
            return new OrderProduct(
                    product.id(),
                    product.nombre(),
                    product.precio(),
                    product.requierePreparacion(),
                    product.cupoMaximoDiario()
            );
        }

        private static OrderProduct fromResponse(ItemResponse product) {
            return new OrderProduct(
                    product.productId(),
                    product.productName(),
                    product.unitPrice(),
                    product.requiresPreparation(),
                    product.dailyCapacity()
            );
        }
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
