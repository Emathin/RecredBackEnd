package com.recred.backend.application;

import com.recred.backend.domain.models.SchoolAssistantAction;
import com.recred.backend.domain.models.SchoolAssistantAnswer;
import com.recred.backend.domain.models.SchoolAssistantCapability;
import com.recred.backend.domain.models.SchoolAssistantSuggestion;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class SchoolAssistantMessageMetadataMapper {

    Map<String, Object> userMetadata(List<SchoolAssistantCapability> capabilities) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("capacidadesPermitidas", capabilityNames(capabilities));
        return metadata;
    }

    Map<String, Object> answerMetadata(
            SchoolAssistantAnswer answer,
            Map<String, Object> additionalMetadata
    ) {
        Map<String, Object> metadata = userMetadata(answer.capabilitiesOrEmpty());
        metadata.put("generadoPorIa", answer.generatedByAi());
        metadata.put("rol", value(answer.roleName()));
        metadata.put("intencion", value(answer.intentName()));
        metadata.put("accion", actionMetadata(answer.action()));
        metadata.put("sugerencias", suggestionsMetadata(answer.suggestionsOrEmpty()));
        metadata.put("datos", answer.dataOrEmpty());
        metadata.putAll(additionalMetadata);

        if (answer.model() != null) {
            metadata.put("modelo", answer.model());
        }

        return metadata;
    }

    private List<String> capabilityNames(List<SchoolAssistantCapability> capabilities) {
        return capabilities.stream()
                .map(SchoolAssistantCapability::name)
                .toList();
    }

    private Map<String, Object> actionMetadata(SchoolAssistantAction action) {
        if (action == null) {
            return Map.of();
        }

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("tipo", action.typeName());
        metadata.put("estado", action.statusName());
        metadata.put("requiereInput", action.requiresInput());
        metadata.put("inputsRequeridos", action.requiredInputNames());
        metadata.put("compraId", value(action.purchaseId()));
        metadata.put("estadoCompra", value(action.purchaseStatusName()));
        metadata.put("codigoRetiro", value(action.withdrawalCode()));
        metadata.put("total", value(action.total()));
        metadata.put("recreo", value(action.recessName()));
        metadata.put("datos", action.dataOrEmpty());
        return metadata;
    }

    private List<Map<String, Object>> suggestionsMetadata(
            List<SchoolAssistantSuggestion> suggestions
    ) {
        return suggestions.stream()
                .map(this::suggestionMetadata)
                .toList();
    }

    private Map<String, Object> suggestionMetadata(SchoolAssistantSuggestion suggestion) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("label", suggestion.label());
        metadata.put("mensaje", suggestion.message());
        metadata.put("tipoAccion", suggestion.actionTypeName());
        return metadata;
    }

    private String value(Object value) {
        return value == null ? "sin_datos" : value.toString();
    }
}
