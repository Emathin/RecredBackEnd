package com.recred.backend.application;

import com.recred.backend.domain.models.ChatMessage;
import com.recred.backend.domain.models.SchoolAssistantContext;
import com.recred.backend.domain.models.SchoolAssistantIntent;
import com.recred.backend.domain.models.SchoolAssistantRole;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

public class SchoolAssistantPurchaseResponder {

    private static final EnumSet<SchoolAssistantIntent> FORBIDDEN_QUICK_PURCHASE_INTENTS =
            EnumSet.of(
                    SchoolAssistantIntent.REPEAT_LAST_PURCHASE,
                    SchoolAssistantIntent.BUY_USUAL,
                    SchoolAssistantIntent.CONFIRM_PURCHASE,
                    SchoolAssistantIntent.CANCEL_ACTION
            );

    private final SchoolAssistantPurchaseRequestParser requestParser;
    private final SchoolAssistantQuickPurchaseHandler quickPurchaseHandler;
    private final SchoolAssistantReplyBuilder replyBuilder;

    public SchoolAssistantPurchaseResponder(
            SchoolAssistantPurchaseRequestParser requestParser,
            SchoolAssistantQuickPurchaseHandler quickPurchaseHandler,
            SchoolAssistantReplyBuilder replyBuilder
    ) {
        this.requestParser = requestParser;
        this.quickPurchaseHandler = quickPurchaseHandler;
        this.replyBuilder = replyBuilder;
    }

    public Optional<SchoolAssistantFixedReply> respond(
            String message,
            SchoolAssistantContext context,
            List<ChatMessage> recentMessages
    ) {
        SchoolAssistantFixedTopicRequest request =
                requestParser.request(message, context, recentMessages);
        return respond(request);
    }

    Optional<SchoolAssistantFixedReply> respond(SchoolAssistantFixedTopicRequest request) {
        if (isForbiddenQuickPurchase(request)) {
            return Optional.of(forbiddenQuickPurchaseReply(request));
        }

        if (!isStudentPurchaseRequest(request)) {
            return Optional.empty();
        }

        return purchaseReply(request);
    }

    boolean supports(SchoolAssistantFixedTopicRequest request) {
        if (isForbiddenQuickPurchase(request)) {
            return true;
        }

        if (!isStudentPurchaseRequest(request)) {
            return false;
        }

        return switch (request.intent()) {
            case REPEAT_LAST_PURCHASE,
                 BUY_USUAL,
                 CONFIRM_PURCHASE,
                 CANCEL_ACTION -> true;
            case SELECT_RECESS -> request.selectedRecessTime().isPresent();
            case SELECT_PURCHASE_DATE -> request.selectedPurchaseDate().isPresent();
            case CANCEL_PURCHASE -> quickPurchaseHandler.hasPendingPurchase(
                    request.recentMessages()
            );
            default -> false;
        };
    }

    private Optional<SchoolAssistantFixedReply> purchaseReply(
            SchoolAssistantFixedTopicRequest request
    ) {
        return switch (request.intent()) {
            case REPEAT_LAST_PURCHASE -> Optional.of(
                    quickPurchaseHandler.repeatLastPurchaseReply(
                            request.context(),
                            request.selectedPurchaseDate()
                    ));
            case BUY_USUAL -> Optional.of(quickPurchaseHandler.buyUsualPurchaseReply(
                    request.context(),
                    request.selectedPurchaseDate()
            ));
            case CONFIRM_PURCHASE -> Optional.of(quickPurchaseHandler.confirmPurchaseReply(
                    request.context(),
                    request.recentMessages()
            ));
            case SELECT_RECESS -> request.selectedRecessTime()
                    .map(recess -> quickPurchaseHandler.selectRecessReply(
                            request.context(),
                            request.recentMessages(),
                            recess
                    ));
            case SELECT_PURCHASE_DATE -> request.selectedPurchaseDate()
                    .map(date -> quickPurchaseHandler.selectPurchaseDateReply(
                            request.context(),
                            request.recentMessages(),
                            date
                    ));
            case CANCEL_ACTION -> Optional.of(quickPurchaseHandler.cancelActionReply(
                    request.recentMessages()
            ));
            case CANCEL_PURCHASE -> cancelPendingDraftReply(request);
            default -> Optional.empty();
        };
    }

    private Optional<SchoolAssistantFixedReply> cancelPendingDraftReply(
            SchoolAssistantFixedTopicRequest request
    ) {
        return quickPurchaseHandler.hasPendingPurchase(request.recentMessages())
                ? Optional.of(quickPurchaseHandler.cancelActionReply(request.recentMessages()))
                : Optional.empty();
    }

    private SchoolAssistantFixedReply forbiddenQuickPurchaseReply(
            SchoolAssistantFixedTopicRequest request
    ) {
        return replyBuilder.reply(
                "Por ahora solo los alumnos pueden hacer compras rapidas "
                        + "desde el asistente.",
                request.intent(),
                replyBuilder.forbiddenQuickPurchaseAction(),
                request.context().role()
        );
    }

    private boolean isForbiddenQuickPurchase(SchoolAssistantFixedTopicRequest request) {
        return request.context().role() != SchoolAssistantRole.ALUMNO
                && FORBIDDEN_QUICK_PURCHASE_INTENTS.contains(request.intent());
    }

    private boolean isStudentPurchaseRequest(SchoolAssistantFixedTopicRequest request) {
        return request.context().role() == SchoolAssistantRole.ALUMNO;
    }
}
