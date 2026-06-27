package com.recred.backend.infrastructure.web;

import com.recred.backend.application.GetScheduledPickupsResponse;
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
    private String pickupSlotDescription;
    private LocalDate pickupDate;
    private List<PurchaseItemDTO> items;

    public ScheduledPickupResponseDTO(GetScheduledPickupsResponse response) {
        this.id = response.id();
        this.studentId = response.studentId();
        this.studentName = response.studentName();
        this.totalAmount = response.totalAmount();
        this.status = response.status();
        this.statusLabel = response.statusLabel();
        this.withdrawalStatus = response.withdrawalStatus();
        this.paymentMethod = response.paymentMethod();
        this.date = response.date();
        this.withdrawalCode = response.withdrawalCode();
        this.pickupSlotId = response.pickupSlotId();
        this.pickupSlotDescription = response.pickupSlotDescription();
        this.pickupDate = response.pickupDate();

        if (response.items() != null) {
            this.items = response.items().stream()
                    .map(item -> new PurchaseItemDTO(
                            item.productId(),
                            item.name(),
                            item.quantity(),
                            item.price()))
                    .collect(Collectors.toList());
        }
    }
}
