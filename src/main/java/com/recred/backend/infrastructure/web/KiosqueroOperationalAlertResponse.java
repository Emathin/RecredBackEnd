package com.recred.backend.infrastructure.web;

import com.recred.backend.domain.models.KiosqueroOperationalAlert;

import java.time.LocalDateTime;
import java.util.UUID;

public record KiosqueroOperationalAlertResponse(
        UUID buffetId,
        String type,
        String severity,
        UUID productId,
        UUID purchaseId,
        LocalDateTime occurredAt,
        String message
) {
    public static KiosqueroOperationalAlertResponse fromDomain(
            KiosqueroOperationalAlert alert
    ) {
        return new KiosqueroOperationalAlertResponse(
                alert.buffetId(),
                alert.type().name(),
                alert.severity().name(),
                alert.productId(),
                alert.purchaseId(),
                alert.occurredAt(),
                alert.message()
        );
    }
}
