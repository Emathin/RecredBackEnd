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
                        .map(OrderResponse::fromPurchase)
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
            List<ItemResponse> items,
            String cancelledByRole
    ) {

        private static OrderResponse fromPurchase(Purchase purchase) {
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
                    PickupSlotResponse.fromPurchase(purchase),
                    StudentResponse.fromStudent(purchase.getStudent()),
                    BuyerResponse.fromBuyer(purchase.getBuyerId(), purchase.getBuyerType()),
                    ItemResponse.fromItems(purchase.getItems()),
                    cancelledByRoleNameFrom(purchase)
            );
        }

        private static String cancelledByRoleNameFrom(Purchase purchase) {
            return purchase.getCancelledByRole() == null 
                    ? null : purchase.getCancelledByRole().name();
        }
    }

    public record PickupSlotResponse(
            UUID id,
            String description,
            LocalTime start,
            LocalTime end
    ) {

        private static PickupSlotResponse fromPurchase(Purchase purchase) {
            if (purchaseHasNoPickupSlot(purchase)) {
                return null;
            }

            return new PickupSlotResponse(
                    purchase.getPickupSlotId(),
                    purchase.getPickupSlotDescription(),
                    purchase.getPickupSlotStart(),
                    purchase.getPickupSlotEnd()
            );
        }

        private static boolean purchaseHasNoPickupSlot(Purchase purchase) {
            return purchase.getPickupSlotId() == null;
        }
    }

    public record StudentResponse(
            UUID id,
            String firstName,
            String lastName,
            String gradeYear,
            String gradeDivision
    ) {

        private static StudentResponse fromStudent(Alumno student) {
            if (student == null) {
                return null;
            }

            Usuario user = student.getUsuario();
            StudentGradeData grade = gradeDataFrom(student);
            return new StudentResponse(
                    student.getId(),
                    firstNameFrom(user),
                    lastNameFrom(user),
                    grade.year(),
                    grade.division()
            );
        }

        private static String firstNameFrom(Usuario user) {
            return user == null ? null : user.getNombre();
        }

        private static String lastNameFrom(Usuario user) {
            return user == null ? null : user.getApellido();
        }

        private static StudentGradeData gradeDataFrom(Alumno student) {
            if (student.getGrado() == null) {
                return new StudentGradeData(null, null);
            }

            return new StudentGradeData(
                    student.getGrado().getAnio(),
                    student.getGrado().getDivision()
            );
        }

        private record StudentGradeData(String year, String division) {
        }
    }

    public record BuyerResponse(UUID id, String type) {

        private static BuyerResponse fromBuyer(UUID buyerId, BuyerType buyerType) {
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

        private static List<ItemResponse> fromItems(Map<Producto, Integer> items) {
            if (items == null) {
                return List.of();
            }

            return items.entrySet().stream()
                    .map(entry -> fromProduct(entry.getKey(), entry.getValue()))
                    .toList();
        }

        private static ItemResponse fromProduct(Producto product, Integer quantity) {
            BigDecimal unitPrice = unitPriceFrom(product);
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

        private static BigDecimal unitPriceFrom(Producto product) {
            return product.getPrecio() == null ? BigDecimal.ZERO : product.getPrecio();
        }
    }
}
