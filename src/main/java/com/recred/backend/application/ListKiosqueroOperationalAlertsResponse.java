package com.recred.backend.application;

import com.recred.backend.domain.models.OperationalAlertSeverity;
import com.recred.backend.domain.models.OperationalAlertType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ListKiosqueroOperationalAlertsResponse(
        List<KiosqueroOperationalAlertDTO> alerts
) {
    public record KiosqueroOperationalAlertDTO(
            UUID buffetId,
            OperationalAlertType type,
            OperationalAlertSeverity severity,
            UUID productId,
            UUID purchaseId,
            LocalDateTime now,
            String message
    ) {
    }
}
