package com.recred.backend.infrastructure.web;


import com.recred.backend.application.GetHistoryByStudentResponse;
import com.recred.backend.application.GetPendingHistoryByStudentResponse;
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

    
    public PurchaseResponseDTO(
            com.recred.backend.application.GetPresentialPurchaseResponse response) {
        this.id = response.id();
        this.studentId = response.studentId();
        this.totalAmount = response.totalAmount();
        this.status = response.status();
        this.statusLabel = response.statusLabel();
        this.paymentMethod = response.paymentMethod();
        this.date = response.date();
        this.tipo = response.tipo();
        this.withdrawalCode = response.withdrawalCode();
        this.pickupSlotDescription = response.pickupSlotDescription();
        this.pickupDate = response.pickupDate();
        if (response.pickupSlotStartTime() != null) {
            this.pickupSlotStartTime = response.pickupSlotStartTime()
                    .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
        }

        if (response.items() != null) {
            this.items = response.items().stream()
                .map(item -> {
                    PurchaseItemDTO itemDTO = new PurchaseItemDTO(
                        item.productId(),
                        item.nombre(),
                        item.quantity(),
                        item.precio()
                    );
                    itemDTO.setRequierePreparacion(item.requierePreparacion());
                    itemDTO.setCupoMaximoDiario(item.cupoMaximoDiario());
                    return itemDTO;
                })
                .collect(java.util.stream.Collectors.toList());
        }
    }
    public PurchaseResponseDTO(GetHistoryByStudentResponse response) {
        this.id = response.id();
        this.studentId = response.studentId();
        this.totalAmount = response.totalAmount();
        this.status = response.status();
        this.statusLabel = response.statusLabel();
        this.paymentMethod = response.paymentMethod();
        this.date = response.date();
        this.tipo = response.tipo();
        this.withdrawalCode = response.withdrawalCode();
        this.pickupSlotDescription = response.pickupSlotDescription();
        this.pickupDate = response.pickupDate();
        if (response.pickupSlotStartTime() != null) {
            this.pickupSlotStartTime = response.pickupSlotStartTime()
                    .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
        }

        if (response.items() != null) {
            this.items = new java.util.ArrayList<>();

            var promoGroups = response.items().stream()
                    .filter(item -> item.promocionId() != null)
                    .collect(Collectors.groupingBy(
                            GetHistoryByStudentResponse.Item::promocionId));

            response.items().stream()
                .filter(item -> item.promocionId() == null)
                .forEach(item -> {
                    this.items.add(purchaseItem(item));
                });

            promoGroups.forEach((promoId, promoItems) -> {
                if (!promoItems.isEmpty()) {
                    String promoName = promoItems.get(0).promocionNombre();
                    int quantity = promoItems.get(0).quantity();

                    BigDecimal unitPrice = promoItems.stream()
                        .map(GetHistoryByStudentResponse.Item::precio)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                    PurchaseItemDTO promoItem = new PurchaseItemDTO(
                        promoId,
                        promoName,
                        quantity,
                        unitPrice
                    );

                    boolean requierePrep = promoItems.stream()
                        .anyMatch(item -> Boolean.TRUE.equals(item.requierePreparacion()));
                    promoItem.setRequierePreparacion(requierePrep);

                    this.items.add(promoItem);
                }
            });
        }
    }

    private PurchaseItemDTO purchaseItem(GetHistoryByStudentResponse.Item item) {
        PurchaseItemDTO itemDTO = new PurchaseItemDTO(
                item.productId(),
                item.nombre(),
                item.quantity(),
                item.precio()
        );
        itemDTO.setRequierePreparacion(item.requierePreparacion());
        itemDTO.setCupoMaximoDiario(item.cupoMaximoDiario());
        return itemDTO;
    }

    public PurchaseResponseDTO(GetPendingHistoryByStudentResponse response) {
        this.id = response.id();
        this.studentId = response.studentId();
        this.totalAmount = response.totalAmount();
        this.status = response.status();
        this.statusLabel = response.statusLabel();
        this.paymentMethod = response.paymentMethod();
        this.date = response.date();
        this.tipo = response.tipo();
        this.withdrawalCode = response.withdrawalCode();
        this.pickupSlotDescription = response.pickupSlotDescription();
        this.pickupDate = response.pickupDate();
        if (response.pickupSlotStartTime() != null) {
            this.pickupSlotStartTime = response.pickupSlotStartTime()
                    .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
        }

        if (response.items() != null) {
            this.items = new java.util.ArrayList<>();

            var promoGroups = response.items().stream()
                    .filter(item -> item.promocionId() != null)
                    .collect(Collectors.groupingBy(
                            GetPendingHistoryByStudentResponse.Item::promocionId));

            response.items().stream()
                .filter(item -> item.promocionId() == null)
                .forEach(item -> {
                    this.items.add(purchaseItem(item));
                });

            promoGroups.forEach((promoId, promoItems) -> {
                if (!promoItems.isEmpty()) {
                    String promoName = promoItems.get(0).promocionNombre();
                    int quantity = promoItems.get(0).quantity();

                    BigDecimal unitPrice = promoItems.stream()
                        .map(GetPendingHistoryByStudentResponse.Item::precio)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                    PurchaseItemDTO promoItem = new PurchaseItemDTO(
                        promoId,
                        promoName,
                        quantity,
                        unitPrice
                    );

                    boolean requierePrep = promoItems.stream()
                        .anyMatch(item -> Boolean.TRUE.equals(item.requierePreparacion()));
                    promoItem.setRequierePreparacion(requierePrep);

                    this.items.add(promoItem);
                }
            });
        }
    }

    private PurchaseItemDTO purchaseItem(GetPendingHistoryByStudentResponse.Item item) {
        PurchaseItemDTO itemDTO = new PurchaseItemDTO(
                item.productId(),
                item.nombre(),
                item.quantity(),
                item.precio()
        );
        itemDTO.setRequierePreparacion(item.requierePreparacion());
        itemDTO.setCupoMaximoDiario(item.cupoMaximoDiario());
        return itemDTO;
    }


}

