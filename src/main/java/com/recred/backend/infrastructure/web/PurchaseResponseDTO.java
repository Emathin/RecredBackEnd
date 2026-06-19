package com.recred.backend.infrastructure.web;

import com.recred.backend.domain.models.Purchase;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
public class PurchaseResponseDTO {
    private UUID id;
    private UUID studentId;
    private BigDecimal totalAmount;
    private String status;
    private String statusLabel;
    private String paymentMethod;
    private LocalDateTime date;
    private List<PurchaseItemDTO> items;
    private String tipo;
    private String withdrawalCode;
    private String pickupSlotDescription;
    private java.time.LocalDate pickupDate;
    private String pickupSlotStartTime;

    public PurchaseResponseDTO(Purchase purchase) {
        this.id = purchase.getId();
        if (purchase.getStudent() != null) {
            this.studentId = purchase.getStudent().getId();
        }
        this.totalAmount = purchase.getTotalAmount();
        if (purchase.getStatus() != null) {
            this.status = purchase.getStatus().name();
            this.statusLabel = purchase.getStatus().getLabel();
        }
        if (purchase.getPaymentMethod() != null) {
            this.paymentMethod = purchase.getPaymentMethod().name();
        }
        this.date = purchase.getDate();
        if (purchase.getTipo() != null) {
            this.tipo = purchase.getTipo().name();
        }
        this.withdrawalCode = purchase.getWithdrawalCode();
        this.pickupSlotDescription = purchase.getPickupSlotDescription();
        this.pickupDate = purchase.getPickupDate();
        if (purchase.getPickupSlotStartTime() != null) {
            this.pickupSlotStartTime = purchase.getPickupSlotStartTime()
                    .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
        }

        if (purchase.getItems() != null) {
            this.items = new java.util.ArrayList<>();

            java.util.Map<UUID, java.util.List<java.util.Map.Entry<
                    com.recred.backend.domain.models.Producto, Integer>>> promoGroups =
                purchase.getItems().entrySet().stream()
                    .filter(entry -> entry.getKey().getPromocionId() != null)
                    .collect(Collectors.groupingBy(entry -> entry.getKey().getPromocionId()));

            purchase.getItems().entrySet().stream()
                .filter(entry -> entry.getKey().getPromocionId() == null)
                .forEach(entry -> {
                    this.items.add(purchaseItem(entry.getKey(), entry.getValue()));
                });

            promoGroups.forEach((promoId, entries) -> {
                if (!entries.isEmpty()) {
                    String promoName = entries.get(0).getKey().getPromocionNombre();
                    int quantity = entries.get(0).getValue();

                    BigDecimal unitPrice = entries.stream()
                        .map(entry -> entry.getKey().getPrecio())
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                    PurchaseItemDTO promoItem = new PurchaseItemDTO(
                        promoId,
                        promoName,
                        quantity,
                        unitPrice
                    );

                    boolean requierePrep = entries.stream()
                        .anyMatch(entry -> Boolean.TRUE.equals(
                                entry.getKey().getRequierePreparacion()
                        ));
                    promoItem.setRequierePreparacion(requierePrep);

                    this.items.add(promoItem);
                }
            });
        }
    }

    private PurchaseItemDTO purchaseItem(
            com.recred.backend.domain.models.Producto product,
            Integer quantity
    ) {
        PurchaseItemDTO item = new PurchaseItemDTO(
                product.getId(),
                product.getNombre(),
                quantity,
                product.getPrecio()
        );
        item.setRequierePreparacion(product.getRequierePreparacion());
        item.setCupoMaximoDiario(product.getCupoMaximoDiario());
        return item;
    }
}
