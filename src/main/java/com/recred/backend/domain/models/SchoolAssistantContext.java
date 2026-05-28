package com.recred.backend.domain.models;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public record SchoolAssistantContext(
        UUID studentId,
        Optional<BigDecimal> balance,
        List<SchoolAssistantPayment> recentPayments,
        List<SchoolAssistantPurchase> recentPurchases,
        List<SchoolCalendarEvent> upcomingEvents
) {
}
