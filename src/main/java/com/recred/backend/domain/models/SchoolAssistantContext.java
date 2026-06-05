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
        List<SchoolCalendarEvent> upcomingEvents,
        SchoolAssistantRole role,
        UUID userId,
        UUID buffetId,
        List<String> facts,
        List<SchoolAssistantCapability> capabilities
) {
    public SchoolAssistantContext(
            UUID studentId,
            Optional<BigDecimal> balance,
            List<SchoolAssistantPayment> recentPayments,
            List<SchoolAssistantPurchase> recentPurchases,
            List<SchoolCalendarEvent> upcomingEvents
    ) {
        this(
                studentId,
                balance,
                recentPayments,
                recentPurchases,
                upcomingEvents,
                SchoolAssistantRole.ALUMNO,
                null,
                null,
                List.of(),
                List.of(
                        SchoolAssistantCapability.SALDO,
                        SchoolAssistantCapability.PAGOS,
                        SchoolAssistantCapability.COMPRAS,
                        SchoolAssistantCapability.EVENTOS
                )
        );
    }
}
