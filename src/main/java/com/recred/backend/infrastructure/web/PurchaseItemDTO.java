package com.recred.backend.infrastructure.web;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class PurchaseItemDTO {
    @NotNull
    private UUID productId;
    private String productName;
    @NotNull
    @Positive
    private Integer quantity;
    private BigDecimal unitPrice;
    private Boolean requierePreparacion;
    private Integer cupoMaximoDiario;

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
