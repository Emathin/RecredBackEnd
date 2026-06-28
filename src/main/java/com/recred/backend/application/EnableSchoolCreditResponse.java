package com.recred.backend.application;

import java.math.BigDecimal;
import java.util.UUID;

public record EnableSchoolCreditResponse(
        UUID creditId,
        BigDecimal amount,
        String status,
        int installments
) {
}
