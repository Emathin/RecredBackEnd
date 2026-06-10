package com.recred.backend.infrastructure.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.RecessTime;
import com.recred.backend.domain.models.SchoolAssistantAction;
import com.recred.backend.domain.models.SchoolAssistantActionStatus;
import com.recred.backend.domain.models.SchoolAssistantActionType;
import com.recred.backend.domain.models.SchoolAssistantRequiredInput;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record SchoolAssistantActionResponse(
        @JsonProperty("tipo")
        SchoolAssistantActionType type,
        @JsonProperty("estado")
        SchoolAssistantActionStatus status,
        @JsonProperty("requiereInput")
        boolean requiresInput,
        @JsonProperty("inputsRequeridos")
        List<SchoolAssistantRequiredInput> requiredInputs,
        @JsonProperty("compraId")
        UUID purchaseId,
        @JsonProperty("estadoCompra")
        EstadoCompra purchaseStatus,
        @JsonProperty("codigoRetiro")
        String withdrawalCode,
        @JsonProperty("total")
        BigDecimal total,
        @JsonProperty("recreo")
        RecessTime recessTime,
        @JsonProperty("datos")
        Map<String, Object> data
) {
    public static SchoolAssistantActionResponse fromDomain(SchoolAssistantAction action) {
        if (action == null) {
            return null;
        }

        return new SchoolAssistantActionResponse(
                action.type(),
                action.status(),
                action.requiresInput(),
                action.requiredInputs(),
                action.purchaseId(),
                action.purchaseStatus(),
                action.withdrawalCode(),
                action.total(),
                action.recessTime(),
                action.data()
        );
    }
}
