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

        if (purchase.getItems() != null) {
            this.items = purchase.getItems().entrySet().stream()
                    .map(entry -> new PurchaseItemDTO(entry.getKey().getId(), entry.getValue()))
                    .collect(Collectors.toList());
        }
    }
}
