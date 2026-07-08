package com.recred.backend.application;

import com.recred.backend.domain.models.SchoolAssistantIntent;
import java.util.EnumSet;

public class SchoolAssistantMessageClassifier {

    private static final EnumSet<SchoolAssistantIntent> PURCHASE_INTENTS = EnumSet.of(
            SchoolAssistantIntent.REPEAT_LAST_PURCHASE,
            SchoolAssistantIntent.BUY_USUAL,
            SchoolAssistantIntent.CONFIRM_PURCHASE,
            SchoolAssistantIntent.SELECT_PURCHASE_DATE,
            SchoolAssistantIntent.SELECT_RECESS,
            SchoolAssistantIntent.CANCEL_ACTION
    );

    private final SchoolAssistantIntentDetector intentDetector;

    public SchoolAssistantMessageClassifier(SchoolAssistantIntentDetector intentDetector) {
        this.intentDetector = intentDetector;
    }

    public SchoolAssistantMessageClassification classify(String message) {
        SchoolAssistantIntent intent = intentDetector.detectIntent(message);
        return new SchoolAssistantMessageClassification(typeFor(intent), intent);
    }

    private SchoolAssistantMessageType typeFor(SchoolAssistantIntent intent) {
        if (intent == SchoolAssistantIntent.CANCEL_PURCHASE) {
            return SchoolAssistantMessageType.PURCHASE_CANCELLATION;
        }

        if (PURCHASE_INTENTS.contains(intent)) {
            return SchoolAssistantMessageType.PURCHASE;
        }

        return SchoolAssistantMessageType.QUESTION;
    }
}
