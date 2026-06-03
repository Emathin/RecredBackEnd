package com.recred.backend.infrastructure.web;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class PurchaseItemDTO {
    private UUID productId;
    private String productName;
    private Integer quantity;
    private BigDecimal unitPrice;

    public PurchaseItemDTO(UUID productId,
                           String productName,
                           Integer quantity,
                           BigDecimal unitPrice) {
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }
}
