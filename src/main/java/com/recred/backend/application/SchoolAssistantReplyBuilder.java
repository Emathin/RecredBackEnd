package com.recred.backend.application;

import com.recred.backend.domain.models.RecessTime;
import com.recred.backend.domain.models.SchoolAssistantAction;
import com.recred.backend.domain.models.SchoolAssistantActionStatus;
import com.recred.backend.domain.models.SchoolAssistantActionType;
import com.recred.backend.domain.models.SchoolAssistantIntent;
import com.recred.backend.domain.models.SchoolAssistantRequiredInput;
import com.recred.backend.domain.models.SchoolAssistantRole;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class SchoolAssistantReplyBuilder {

    private static final String INTENT_KEY = "intencionChatbot";

    private final SchoolAssistantSuggestionFactory suggestionFactory;

    SchoolAssistantReplyBuilder(SchoolAssistantSuggestionFactory suggestionFactory) {
        this.suggestionFactory = suggestionFactory;
    }

    Map<String, Object> metadata(String intent) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put(INTENT_KEY, intent);
        return metadata;
    }

    SchoolAssistantFixedReply reply(
            String answer,
            SchoolAssistantIntent intent,
            SchoolAssistantAction action
    ) {
        return reply(answer, intent, action, SchoolAssistantRole.ALUMNO);
    }

    SchoolAssistantFixedReply reply(
            String answer,
            SchoolAssistantIntent intent,
            SchoolAssistantAction action,
            SchoolAssistantRole role
    ) {
        return new SchoolAssistantFixedReply(
                answer,
                metadata(intent.name()),
                intent,
                action,
                suggestionFactory.suggestions(role, intent, action),
                action == null ? Map.of() : action.data()
        );
    }

    SchoolAssistantAction informativeAction(
            SchoolAssistantActionType type,
            Map<String, Object> data
    ) {
        return SchoolAssistantAction.informative(type, data);
    }

    SchoolAssistantAction forbiddenQuickPurchaseAction() {
        return advanceAction(
                SchoolAssistantActionStatus.NO_PERMITIDA,
                false,
                List.of(),
                Map.of(),
                null
        );
    }

    SchoolAssistantAction failedAdvanceAction(Map<String, Object> data) {
        return advanceAction(
                SchoolAssistantActionStatus.FALLIDA,
                false,
                List.of(),
                data,
                null
        );
    }

    SchoolAssistantAction advanceAction(
            SchoolAssistantActionStatus status,
            boolean requiresInput,
            List<SchoolAssistantRequiredInput> requiredInputs,
            Map<String, Object> data,
            RecessTime recessTime
    ) {
        return new SchoolAssistantAction(
                SchoolAssistantActionType.COMPRA_ANTICIPADA,
                status,
                requiresInput,
                requiredInputs,
                null,
                null,
                null,
                null,
                recessTime,
                data
        );
    }
}
