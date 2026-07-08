package com.recred.backend.application;

import com.recred.backend.domain.models.SchoolAssistantAction;
import com.recred.backend.domain.models.SchoolAssistantAnswer;
import com.recred.backend.domain.models.SchoolAssistantSuggestion;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record AskSchoolAssistantResponse(
        UUID sessionId,
        String answer,
        List<String> capabilities,
        LocalDateTime dateTime,
        boolean generatedByAi,
        String model,
        String role,
        String intent,
        ActionResponse action,
        List<SuggestionResponse> suggestions,
        Map<String, Object> data
) {

    public static AskSchoolAssistantResponse from(SchoolAssistantAnswer answer) {
        return new AskSchoolAssistantResponse(
                answer.sessionId(),
                answer.answer(),
                answer.capabilityNames(),
                answer.dateTime(),
                answer.generatedByAi(),
                answer.model(),
                answer.roleName(),
                answer.intentName(),
                ActionResponse.from(answer.action()),
                answer.suggestionsOrEmpty().stream()
                        .map(SuggestionResponse::from)
                        .toList(),
                answer.dataOrEmpty()
        );
    }

    public record ActionResponse(
            String type,
            String status,
            boolean requiresInput,
            List<String> requiredInputs,
            UUID purchaseId,
            String purchaseStatus,
            String withdrawalCode,
            BigDecimal total,
            String recessTime,
            Map<String, Object> data
    ) {

        private static ActionResponse from(SchoolAssistantAction action) {
            if (action == null) {
                return null;
            }

            return new ActionResponse(
                    action.typeName(),
                    action.statusName(),
                    action.requiresInput(),
                    action.requiredInputNames(),
                    action.purchaseId(),
                    action.purchaseStatusName(),
                    action.withdrawalCode(),
                    action.total(),
                    action.recessLabel(),
                    action.dataOrEmpty()
            );
        }
    }

    public record SuggestionResponse(
            String label,
            String message,
            String actionType
    ) {

        private static SuggestionResponse from(SchoolAssistantSuggestion suggestion) {
            return new SuggestionResponse(
                    suggestion.label(),
                    suggestion.message(),
                    suggestion.actionTypeName()
            );
        }
    }
}
