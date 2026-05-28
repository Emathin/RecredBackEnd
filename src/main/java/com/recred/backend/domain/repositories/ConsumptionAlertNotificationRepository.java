package com.recred.backend.domain.repositories;

import com.recred.backend.domain.models.ConsumptionAlert;
import com.recred.backend.domain.models.ConsumptionAlertType;
import java.time.LocalDateTime;
import java.util.UUID;

public interface ConsumptionAlertNotificationRepository {

    boolean hasAnyAlertBetween(
            UUID studentId,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime
    );

    boolean hasAlertSince(
            UUID studentId,
            ConsumptionAlertType alertType,
            LocalDateTime since
    );

    void save(ConsumptionAlert alert);
}
