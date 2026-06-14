package com.recred.backend.infrastructure.web;

import java.math.BigDecimal;
import java.util.UUID;

public record TopUpRequest(
    UUID studentId,
    BigDecimal amount
) {
}
