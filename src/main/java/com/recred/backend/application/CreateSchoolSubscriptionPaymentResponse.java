package com.recred.backend.application;

import java.math.BigDecimal;

public record CreateSchoolSubscriptionPaymentResponse(
        String paymentUrl,
        BigDecimal price,
        String currency
) {
}
