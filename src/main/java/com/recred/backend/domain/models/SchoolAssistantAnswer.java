package com.recred.backend.domain.models;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record SchoolAssistantAnswer(
        UUID sessionId,
        String answer,
        List<SchoolAssistantCapability> capabilities,
        LocalDateTime dateTime,
        boolean generatedByAi,
        String model
) {
}
