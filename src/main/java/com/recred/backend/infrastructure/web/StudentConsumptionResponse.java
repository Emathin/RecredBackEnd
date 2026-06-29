package com.recred.backend.infrastructure.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.recred.backend.application.GetLoggedUserConsumptionDetailResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record StudentConsumptionResponse(
        @JsonProperty("alumnoId")
        UUID studentId,
        @JsonProperty("desde")
        LocalDate from,
        @JsonProperty("hasta")
        LocalDate to,
        @JsonProperty("montoTotal")
        BigDecimal totalAmount,
        @JsonProperty("cantidadCompras")
        int purchaseCount,
        @JsonProperty("compras")
        List<StudentConsumptionPurchaseResponse> purchases
) {
    public static StudentConsumptionResponse fromApplication(
            GetLoggedUserConsumptionDetailResponse detail
    ) {
        return new StudentConsumptionResponse(
                detail.studentId(),
                detail.from(),
                detail.to(),
                detail.totalAmount(),
                detail.purchaseCount(),
                detail.purchases().stream()
                        .map(StudentConsumptionPurchaseResponse::fromApplication)
                        .toList()
        );
    }
}
