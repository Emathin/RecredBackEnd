package com.recred.backend.application;

import java.math.BigDecimal;
import java.util.UUID;

public record GenerateTopUpLinkRequest(
        UUID studentId,
        BigDecimal amount
) {
}
