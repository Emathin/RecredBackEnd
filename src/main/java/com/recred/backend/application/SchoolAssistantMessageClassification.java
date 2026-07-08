package com.recred.backend.application;

import com.recred.backend.domain.models.SchoolAssistantIntent;

public record SchoolAssistantMessageClassification(
        SchoolAssistantMessageType type,
        SchoolAssistantIntent intent
) {

    public boolean isQuestion() {
        return type == SchoolAssistantMessageType.QUESTION;
    }

    public boolean isPurchase() {
        return type == SchoolAssistantMessageType.PURCHASE;
    }

    public boolean isPurchaseCancellation() {
        return type == SchoolAssistantMessageType.PURCHASE_CANCELLATION;
    }
}
