package com.recred.backend.infrastructure.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.recred.backend.application.GetLoggedUserConsumptionDetailResponse.PurchaseResponse;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record StudentConsumptionPurchaseResponse(
        @JsonProperty("compraId")
        UUID purchaseId,
        @JsonProperty("fechaHora")
        LocalDateTime dateTime,
        @JsonProperty("estado")
        String status,
        @JsonProperty("medioPago")
        String paymentMethod,
        @JsonProperty("montoTotal")
        BigDecimal totalAmount,
        @JsonProperty("buffetId")
        UUID buffetId,
        @JsonProperty("buffet")
        String buffetName,
        @JsonProperty("productos")
        List<StudentConsumptionProductResponse> products
) {
    public static StudentConsumptionPurchaseResponse fromApplication(
            PurchaseResponse purchase
    ) {
        return new StudentConsumptionPurchaseResponse(
                purchase.purchaseId(),
                purchase.dateTime(),
                purchase.status(),
                purchase.paymentMethod(),
                purchase.totalAmount(),
                purchase.buffetId(),
                purchase.buffetName(),
                purchase.products().stream()
                        .map(StudentConsumptionProductResponse::fromApplication)
                        .toList()
        );
    }
}
