package com.recred.backend.application;

import java.util.UUID;

public record GenerateSummaryNotificationResponse(
        UUID userId,
        String title,
        String message,
        String type
) {
}
