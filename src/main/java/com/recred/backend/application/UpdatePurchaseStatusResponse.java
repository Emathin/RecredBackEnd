package com.recred.backend.application;

import java.time.LocalDateTime;
import java.util.UUID;

public record UpdatePurchaseStatusResponse(
        UUID id,
        UUID studentId,
        String status,
        String label,
        LocalDateTime date
) {
}
