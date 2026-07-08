package com.recred.backend.application;

import com.recred.backend.domain.exceptions.InvalidChatMessageException;
import com.recred.backend.domain.models.SchoolAssistantContext;
import java.time.Clock;
import java.time.LocalDateTime;

public class AskSchoolAssistant {

    private final SchoolAssistantConversationService conversationService;
    private final SchoolAssistantContextResolver contextResolver;
    private final SchoolAssistantQuestionResponder questionResponder;
    private final SchoolAssistantMessageClassifier messageClassifier;
    private final SchoolAssistantPurchaseResponder purchaseResponder;
    private final SchoolAssistantPurchaseCancellationResponder purchaseCancellationResponder;
    private final Clock clock;

    public AskSchoolAssistant(
            SchoolAssistantConversationService conversationService,
            SchoolAssistantContextResolver contextResolver,
            SchoolAssistantQuestionResponder questionResponder,
            SchoolAssistantMessageClassifier messageClassifier,
            SchoolAssistantPurchaseResponder purchaseResponder,
            SchoolAssistantPurchaseCancellationResponder purchaseCancellationResponder,
            Clock clock
    ) {
        this.conversationService = conversationService;
        this.contextResolver = contextResolver;
        this.questionResponder = questionResponder;
        this.messageClassifier = messageClassifier;
        this.purchaseResponder = purchaseResponder;
        this.purchaseCancellationResponder = purchaseCancellationResponder;
        this.clock = clock;
    }

    public AskSchoolAssistantResponse execute(AskSchoolAssistantRequest request) {
        requireValidRequest(request);

        SchoolAssistantResolvedContext resolvedContext = resolveAssistantContext(request);
        String message = validatedMessage(request);
        LocalDateTime now = currentTime();
        SchoolAssistantConversation conversation = startConversation(
                request,
                resolvedContext,
                now
        );
        SchoolAssistantAnswerResult answerResult = answerMessage(
                conversation,
                message,
                resolvedContext.context(),
                now
        );

        saveConversationTurn(
                conversation,
                message,
                resolvedContext.context(),
                answerResult,
                now
        );

        return responseFrom(answerResult);
    }

    private void requireValidRequest(AskSchoolAssistantRequest request) {
        if (request == null || request.role() == null) {
            throw new IllegalArgumentException("request and role are required");
        }
    }

    private String validatedMessage(AskSchoolAssistantRequest request) {
        String message = request.message() == null ? "" : request.message().trim();

        if (message.isBlank()) {
            throw new InvalidChatMessageException();
        }

        return message;
    }

    private LocalDateTime currentTime() {
        return LocalDateTime.now(clock);
    }

    private SchoolAssistantResolvedContext resolveAssistantContext(
            AskSchoolAssistantRequest request
    ) {
        return contextResolver.resolve(
                request.role(),
                request.cognitoSubject()
        );
    }

    private SchoolAssistantConversation startConversation(
            AskSchoolAssistantRequest request,
            SchoolAssistantResolvedContext resolvedContext,
            LocalDateTime now
    ) {
        return conversationService.start(
                resolvedContext.conversationOwner(),
                request.sessionId(),
                now
        );
    }

    private void saveConversationTurn(
            SchoolAssistantConversation conversation,
            String message,
            SchoolAssistantContext context,
            SchoolAssistantAnswerResult answerResult,
            LocalDateTime now
    ) {
        conversationService.saveConversation(
                conversation,
                message,
                context.capabilities(),
                answerResult.answer(),
                answerResult.additionalMetadata(),
                now
        );
    }

    private AskSchoolAssistantResponse responseFrom(SchoolAssistantAnswerResult answerResult) {
        return AskSchoolAssistantResponse.from(answerResult.answer());
    }

    private SchoolAssistantAnswerResult answerMessage(
            SchoolAssistantConversation conversation,
            String message,
            SchoolAssistantContext context,
            LocalDateTime now
    ) {
        SchoolAssistantMessageClassification classification = messageClassifier.classify(message);

        if (classification.isPurchase()) {
            return purchaseAnswer(conversation, message, context, now);
        }

        if (classification.isPurchaseCancellation()) {
            return purchaseCancellationAnswer(conversation, message, context, now);
        }

        return questionAnswer(conversation, message, context, now);
    }

    private SchoolAssistantAnswerResult purchaseAnswer(
            SchoolAssistantConversation conversation,
            String message,
            SchoolAssistantContext context,
            LocalDateTime now
    ) {
        return purchaseResponder.respond(message, context, conversation.recentMessages())
                .map(reply -> fixedAnswer(conversation, reply, context, now))
                .orElseGet(() -> questionAnswer(conversation, message, context, now));
    }

    private SchoolAssistantAnswerResult purchaseCancellationAnswer(
            SchoolAssistantConversation conversation,
            String message,
            SchoolAssistantContext context,
            LocalDateTime now
    ) {
        return purchaseResponder.respond(message, context, conversation.recentMessages())
                .or(() -> purchaseCancellationResponder.respond(
                        message,
                        context,
                        conversation.recentMessages()
                ))
                .map(reply -> fixedAnswer(conversation, reply, context, now))
                .orElseGet(() -> questionAnswer(conversation, message, context, now));
    }

    private SchoolAssistantAnswerResult questionAnswer(
            SchoolAssistantConversation conversation,
            String message,
            SchoolAssistantContext context,
            LocalDateTime now
    ) {
        return questionResponder.respond(conversation, message, context, now);
    }

    private SchoolAssistantAnswerResult fixedAnswer(
            SchoolAssistantConversation conversation,
            SchoolAssistantFixedReply reply,
            SchoolAssistantContext context,
            LocalDateTime now
    ) {
        return SchoolAssistantAnswerResult.fromFixedReply(conversation, reply, context, now);
    }

}
