package com.recred.backend.application;

import com.recred.backend.domain.models.ChatMessage;
import com.recred.backend.domain.models.RecessTime;
import com.recred.backend.domain.models.SchoolAssistantContext;
import com.recred.backend.domain.models.SchoolAssistantIntent;
import com.recred.backend.domain.models.SchoolAssistantRole;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class SchoolAssistantPurchaseCancellationResponder {

    private final SchoolAssistantIntentDetector intentDetector;
    private final SchoolAssistantPurchaseCancellationHandler purchaseCancellationHandler;

    public SchoolAssistantPurchaseCancellationResponder(
            SchoolAssistantIntentDetector intentDetector,
            SchoolAssistantPurchaseCancellationHandler purchaseCancellationHandler
    ) {
        this.intentDetector = intentDetector;
        this.purchaseCancellationHandler = purchaseCancellationHandler;
    }

    public Optional<SchoolAssistantFixedReply> respond(
            String message,
            SchoolAssistantContext context,
            List<ChatMessage> recentMessages
    ) {
        return respond(request(message, context, recentMessages));
    }

    Optional<SchoolAssistantFixedReply> respond(SchoolAssistantFixedTopicRequest request) {
        if (!supports(request)) {
            return Optional.empty();
        }

        return Optional.of(purchaseCancellationHandler.cancelPurchaseReply(
                request.context(),
                request.recentMessages()
        ));
    }

    boolean supports(SchoolAssistantFixedTopicRequest request) {
        return request.context().role() == SchoolAssistantRole.ALUMNO
                && request.intent() == SchoolAssistantIntent.CANCEL_PURCHASE;
    }

    private SchoolAssistantFixedTopicRequest request(
            String message,
            SchoolAssistantContext context,
            List<ChatMessage> recentMessages
    ) {
        return new SchoolAssistantFixedTopicRequest(
                intentDetector.detectIntent(message),
                context,
                recentMessages,
                Optional.<LocalDate>empty(),
                Optional.<RecessTime>empty()
        );
    }
}
