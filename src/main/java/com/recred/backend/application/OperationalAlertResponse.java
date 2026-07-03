package com.recred.backend.application;

import com.recred.backend.domain.models.KiosqueroOperationalAlert;

import java.time.LocalDateTime;
import java.util.UUID;

public record OperationalAlertResponse(
        UUID buffetId,
        String type,
        String severity,
        UUID productId,
        UUID purchaseId,
        LocalDateTime occurredAt,
        String message
) {

    public static OperationalAlertResponse fromDomain(KiosqueroOperationalAlert alert) {
        return new OperationalAlertResponse(
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
