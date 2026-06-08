package com.recred.backend.infrastructure.web;

import com.recred.backend.domain.models.Purchase;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
public class ScheduledPickupResponseDTO {
    private UUID id;
    private UUID studentId;
    private String studentName;
    private BigDecimal totalAmount;
    private String status;
    private String statusLabel;
    private String withdrawalStatus;
    private String paymentMethod;
    private LocalDateTime date;
    private String withdrawalCode;
    private UUID pickupSlotId;
    private LocalDate pickupDate;
    private List<PurchaseItemDTO> items;

    public ScheduledPickupResponseDTO(Purchase purchase) {
        this.id = purchase.getId();
        if (purchase.getStudent() != null) {
            this.studentId = purchase.getStudent().getId();
            this.studentName = purchase.getStudent().getNombre() + " "
                    + purchase.getStudent().getApellido();
        }
        this.totalAmount = purchase.getTotalAmount();
        if (purchase.getStatus() != null) {
            this.status = purchase.getStatus().name();
            this.statusLabel = purchase.getStatus().getLabel();
        }
        if (purchase.getWithdrawalStatus() != null) {
            this.withdrawalStatus = purchase.getWithdrawalStatus().name();
        }
        if (purchase.getPaymentMethod() != null) {
            this.paymentMethod = purchase.getPaymentMethod().name();
        }
        this.date = purchase.getDate();
        this.withdrawalCode = purchase.getWithdrawalCode();
        this.pickupSlotId = purchase.getPickupSlotId();
        this.pickupDate = purchase.getPickupDate();

        if (purchase.getItems() != null) {
            this.items = purchase.getItems().entrySet().stream()
                    .map(entry -> new PurchaseItemDTO(
                            entry.getKey().getId(),
                            entry.getKey().getNombre(),
                            entry.getValue(),
                            entry.getKey().getPrecio()))
                    .collect(Collectors.toList());
        }
    }
}
