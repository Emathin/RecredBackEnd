package com.recred.backend.application;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferBalanceBetweenChildrenRequest(
        String cognitoSub,
        UUID fromStudentId,
        UUID toStudentId,
        BigDecimal amount
) {
}
