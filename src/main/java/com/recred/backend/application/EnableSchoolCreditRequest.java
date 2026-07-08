package com.recred.backend.application;

import java.math.BigDecimal;
import java.util.UUID;

public record EnableSchoolCreditRequest(
        UUID tutorId,
        UUID studentId,
        BigDecimal amount,
        Integer installments
) {
}
