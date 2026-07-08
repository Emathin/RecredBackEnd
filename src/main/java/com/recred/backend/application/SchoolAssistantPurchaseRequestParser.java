package com.recred.backend.application;

import com.recred.backend.domain.models.ChatMessage;
import com.recred.backend.domain.models.RecessTime;
import com.recred.backend.domain.models.SchoolAssistantContext;
import com.recred.backend.domain.models.SchoolAssistantIntent;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class SchoolAssistantPurchaseRequestParser {

    private final SchoolAssistantIntentDetector intentDetector;
    private final Clock clock;

    public SchoolAssistantPurchaseRequestParser(
            SchoolAssistantIntentDetector intentDetector,
            Clock clock
    ) {
        this.intentDetector = intentDetector;
        this.clock = clock;
    }

    SchoolAssistantFixedTopicRequest request(
            String message,
            SchoolAssistantContext context,
            List<ChatMessage> recentMessages
    ) {
        SchoolAssistantIntent intent = intentDetector.detectIntent(message);
        LocalDate today = LocalDate.now(clock);
        Optional<LocalDate> selectedPurchaseDate = intentDetector.purchaseDate(message, today);
        Optional<RecessTime> selectedRecessTime = intentDetector.recessTime(message);
        return new SchoolAssistantFixedTopicRequest(
                intent,
                context,
                recentMessages,
                selectedPurchaseDate,
                selectedRecessTime
        );
    }
}
