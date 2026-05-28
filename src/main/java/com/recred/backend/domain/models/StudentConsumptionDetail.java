package com.recred.backend.domain.models;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record StudentConsumptionDetail(
        UUID studentId,
        LocalDate from,
        LocalDate to,
        BigDecimal totalAmount,
        int purchaseCount,
        List<StudentConsumptionPurchase> purchases
) {
}
