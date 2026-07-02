package com.recred.backend.infrastructure.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.recred.backend.application.AskSchoolAssistantResponse.ActionResponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record SchoolAssistantActionResponse(
        @JsonProperty("tipo")
        String type,
        @JsonProperty("estado")
        String status,
        @JsonProperty("requiereInput")
        boolean requiresInput,
        @JsonProperty("inputsRequeridos")
        List<String> requiredInputs,
        @JsonProperty("compraId")
        UUID purchaseId,
        @JsonProperty("estadoCompra")
        String purchaseStatus,
        @JsonProperty("codigoRetiro")
        String withdrawalCode,
        @JsonProperty("total")
        BigDecimal total,
        @JsonProperty("recreo")
        String recessTime,
        @JsonProperty("datos")
        Map<String, Object> data
) {
    public static SchoolAssistantActionResponse fromApplication(ActionResponse action) {
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
