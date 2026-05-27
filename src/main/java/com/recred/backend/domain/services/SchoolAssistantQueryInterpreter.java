package com.recred.backend.domain.services;

import com.recred.backend.domain.models.ChatMessage;
import com.recred.backend.domain.models.SchoolAssistantQuery;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SchoolAssistantQueryInterpreter {
    Optional<SchoolAssistantQuery> interpret(
            String question,
            LocalDate currentDate,
            List<ChatMessage> recentMessages
    );
}
