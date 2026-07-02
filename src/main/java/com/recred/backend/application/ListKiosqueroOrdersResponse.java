package com.recred.backend.application;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.BuyerType;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.Usuario;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ListKiosqueroOrdersResponse(
        List<OrderResponse> orders
) {

    public static ListKiosqueroOrdersResponse from(List<Purchase> purchases) {
        return new ListKiosqueroOrdersResponse(
                purchases.stream()
                        .map(OrderResponse::from)
                        .toList()
        );
    }

    public record OrderResponse(
            UUID id,
            UUID buffetId,
            String type,
            String status,
            String statusLabel,
            BigDecimal totalAmount,
            String paymentMethod,
            LocalDateTime date,
            String withdrawalCode,
            LocalDate pickupDate,
            PickupSlotResponse pickupSlot,
            StudentResponse student,
            BuyerResponse buyer,
            List<ItemResponse> items
    ) {

        private static OrderResponse from(Purchase purchase) {
            return new OrderResponse(
                    purchase.getId(),
                    purchase.getBuffetId(),
                    purchase.typeName(),
                    purchase.statusName(),
                    purchase.statusLabel(),
                    purchase.getTotalAmount(),
                    purchase.paymentMethodName(),
                    purchase.getDate(),
                    purchase.getWithdrawalCode(),
                    purchase.getPickupDate(),
                    PickupSlotResponse.from(purchase),
                    StudentResponse.from(purchase.getStudent()),
                    BuyerResponse.from(purchase.getBuyerId(), purchase.getBuyerType()),
                    ItemResponse.from(purchase.getItems())
            );
        }
    }

    public record PickupSlotResponse(
            UUID id,
            String description,
            LocalTime start,
            LocalTime end
    ) {

        private static PickupSlotResponse from(Purchase purchase) {
            if (purchase.getPickupSlotId() == null) {
                return null;
            }

            return new PickupSlotResponse(
                    purchase.getPickupSlotId(),
                    purchase.getPickupSlotDescription(),
                    purchase.getPickupSlotStart(),
                    purchase.getPickupSlotEnd()
            );
        }
    }

    public record StudentResponse(
            UUID id,
            String firstName,
            String lastName,
            String gradeYear,
            String gradeDivision
    ) {

        private static StudentResponse from(Alumno student) {
            if (student == null) {
                return null;
            }

            Usuario user = student.getUsuario();
            return new StudentResponse(
                    student.getId(),
                    user == null ? null : user.getNombre(),
                    user == null ? null : user.getApellido(),
                    student.getGrado() == null ? null : student.getGrado().getAnio(),
                    student.getGrado() == null ? null : student.getGrado().getDivision()
            );
        }
    }

    public record BuyerResponse(UUID id, String type) {

        private static BuyerResponse from(UUID buyerId, BuyerType buyerType) {
            return new BuyerResponse(buyerId, buyerType == null ? null : buyerType.name());
        }
    }

    public record ItemResponse(
            UUID productId,
            String productName,
            Integer quantity,
            BigDecimal unitPrice,
            BigDecimal subtotal,
            Boolean requiresPreparation,
            Integer dailyCapacity
    ) {

        private static List<ItemResponse> from(Map<Producto, Integer> items) {
            if (items == null) {
                return List.of();
            }

            return items.entrySet().stream()
                    .map(entry -> from(entry.getKey(), entry.getValue()))
                    .toList();
        }

        private static ItemResponse from(Producto product, Integer quantity) {
            BigDecimal unitPrice = product.getPrecio() == null
                    ? BigDecimal.ZERO
                    : product.getPrecio();
            return new ItemResponse(
                    product.getId(),
                    product.getNombre(),
                    quantity,
                    unitPrice,
                    unitPrice.multiply(BigDecimal.valueOf(quantity)),
                    product.getRequierePreparacion(),
                    product.getCupoMaximoDiario()
            );
        }
    }
}
