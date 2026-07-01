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

    public String typeName() {
        return type == null ? null : type.name();
    }

    public String statusName() {
        return status == null ? null : status.name();
    }

    public List<String> requiredInputNames() {
        return requiredInputsOrEmpty().stream()
                .map(Enum::name)
                .toList();
    }

    public List<SchoolAssistantRequiredInput> requiredInputsOrEmpty() {
        return requiredInputs == null ? List.of() : requiredInputs;
    }

    public String purchaseStatusName() {
        return purchaseStatus == null ? null : purchaseStatus.name();
    }

    public String recessLabel() {
        return recessTime == null ? null : recessTime.getLabel();
    }

    public String recessName() {
        return recessTime == null ? null : recessTime.name();
    }

    public Map<String, Object> dataOrEmpty() {
        return data == null ? Map.of() : data;
    }
}
