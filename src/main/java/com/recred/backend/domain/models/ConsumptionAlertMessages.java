package com.recred.backend.domain.models;

import java.math.BigDecimal;

public record ConsumptionAlertMessages(
        String title,
        String tutorTitle,
        String studentMessage,
        String tutorMessage,
        String suggestedAction,
        BigDecimal confidence,
        String model
) {
}
