package com.recred.backend.domain.models;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record SchoolAssistantAction(
        SchoolAssistantActionType type,
        SchoolAssistantActionStatus status,
        boolean requiresInput,
        List<SchoolAssistantRequiredInput> requiredInputs,
        UUID purchaseId,
        EstadoCompra purchaseStatus,
        String withdrawalCode,
        BigDecimal total,
        RecessTime recessTime,
        Map<String, Object> data
) {
    public static SchoolAssistantAction informative(
            SchoolAssistantActionType type,
            Map<String, Object> data
    ) {
        return new SchoolAssistantAction(
                type,
                SchoolAssistantActionStatus.INFORMATIVA,
                false,
                List.of(),
                null,
                null,
                null,
                null,
                null,
                data
        );
    }
}
