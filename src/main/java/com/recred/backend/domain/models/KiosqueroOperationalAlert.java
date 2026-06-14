package com.recred.backend.domain.models;

import java.time.LocalDateTime;
import java.util.UUID;

public record KiosqueroOperationalAlert(
        UUID buffetId,
        OperationalAlertType type,
        OperationalAlertSeverity severity,
        UUID productId,
        UUID purchaseId,
        LocalDateTime occurredAt,
        String message
) {
}
