package com.recred.backend.domain.models;

import java.util.List;
import java.util.Optional;

public record SchoolAssistantQuery(
        List<SchoolAssistantCapability> capabilities,
        Optional<DateRange> paymentDateRange,
        Optional<DateRange> eventDateRange,
        int paymentLimit,
        boolean requiresAiResponse
) {
}
