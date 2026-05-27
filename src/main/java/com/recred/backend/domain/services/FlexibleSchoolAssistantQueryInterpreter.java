package com.recred.backend.domain.services;

import com.recred.backend.domain.models.ChatMessage;
import com.recred.backend.domain.models.SchoolAssistantQuery;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class FlexibleSchoolAssistantQueryInterpreter implements SchoolAssistantQueryInterpreter {

    private final SchoolAssistantLocalQueryInterpreter localInterpreter;
    private final SchoolAssistantAiQueryInterpreter aiInterpreter;

    public FlexibleSchoolAssistantQueryInterpreter(
            SchoolAssistantLocalQueryInterpreter localInterpreter,
            SchoolAssistantAiQueryInterpreter aiInterpreter
    ) {
        this.localInterpreter = localInterpreter;
        this.aiInterpreter = aiInterpreter;
    }

    @Override
    public Optional<SchoolAssistantQuery> interpret(
            String question,
            LocalDate currentDate,
            List<ChatMessage> recentMessages
    ) {
        return localInterpreter.interpret(question, currentDate, recentMessages)
                .or(() -> aiInterpreter.interpret(question, currentDate, recentMessages))
                .or(() -> Optional.of(localInterpreter.fallback(question, currentDate)));
    }
}
