package com.recred.backend.application;

import com.recred.backend.domain.models.ChatMessage;
import com.recred.backend.domain.models.RecessTime;
import com.recred.backend.domain.models.SchoolAssistantActionType;
import com.recred.backend.domain.models.SchoolAssistantContext;
import com.recred.backend.domain.models.SchoolAssistantIntent;
import com.recred.backend.domain.models.SchoolAssistantRole;
import com.recred.backend.domain.repositories.ProductRepository;
import com.recred.backend.domain.repositories.PurchaseRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

public class SchoolAssistantFixedTopics {

    private static final EnumSet<SchoolAssistantIntent> FORBIDDEN_QUICK_PURCHASE_INTENTS =
            EnumSet.of(
                    SchoolAssistantIntent.REPEAT_LAST_PURCHASE,
                    SchoolAssistantIntent.BUY_USUAL,
                    SchoolAssistantIntent.CONFIRM_PURCHASE,
                    SchoolAssistantIntent.CANCEL_ACTION
            );
    private static final EnumSet<SchoolAssistantIntent> KIOSQUERO_PENDING_ORDER_INTENTS =
            EnumSet.of(
                    SchoolAssistantIntent.PENDING_ORDERS,
                    SchoolAssistantIntent.ORDER_STATUS,
                    SchoolAssistantIntent.WITHDRAWAL_CODE
            );

    private final SchoolAssistantIntentDetector intentDetector;
    private final SchoolAssistantQuickPurchaseHandler quickPurchaseHandler;
    private final SchoolAssistantPurchaseCancellationHandler purchaseCancellationHandler;
    private final SchoolAssistantFixedTopicResponses responses;
    private final List<SchoolAssistantFixedTopicHandler> handlers;
    private final Clock clock;

    public SchoolAssistantFixedTopics(
            PurchaseRepository purchaseRepository,
            ProductRepository productRepository,
            CreateAssistantAdvancePurchase createAssistantAdvancePurchase,
            CancelPresentialPurchase cancelPresentialPurchase,
            Clock clock
    ) {
        SchoolAssistantReplyFormatter replyFormatter = new SchoolAssistantReplyFormatter();
        SchoolAssistantContentFormatter contentFormatter = new SchoolAssistantContentFormatter(
                productRepository,
                replyFormatter
        );
        SchoolAssistantTutorFactsFormatter tutorFactsFormatter =
                new SchoolAssistantTutorFactsFormatter();
        this.intentDetector = new SchoolAssistantIntentDetector();
        SchoolAssistantSuggestionFactory suggestionFactory =
                new SchoolAssistantSuggestionFactory();
        SchoolAssistantPendingPurchaseParser pendingPurchaseParser =
                new SchoolAssistantPendingPurchaseParser(intentDetector);
        SchoolAssistantPurchasePatternService purchasePatternService =
                new SchoolAssistantPurchasePatternService(
                        purchaseRepository,
                        productRepository,
                        contentFormatter
                );
        SchoolAssistantReplyBuilder replyBuilder = new SchoolAssistantReplyBuilder(
                suggestionFactory
        );
        this.clock = clock;
        this.quickPurchaseHandler = new SchoolAssistantQuickPurchaseHandler(
                purchasePatternService,
                productRepository,
                createAssistantAdvancePurchase,
                clock,
                replyFormatter,
                contentFormatter,
                new SchoolAssistantPurchaseFailureMessages(),
                suggestionFactory,
                pendingPurchaseParser,
                replyBuilder
        );
        this.purchaseCancellationHandler = new SchoolAssistantPurchaseCancellationHandler(
                purchaseRepository,
                cancelPresentialPurchase,
                contentFormatter,
                replyBuilder
        );
        this.responses = new SchoolAssistantFixedTopicResponses(
                purchaseRepository,
                productRepository,
                purchasePatternService,
                replyFormatter,
                contentFormatter,
                tutorFactsFormatter,
                replyBuilder
        );
        this.handlers = handlers();
    }

    public Optional<SchoolAssistantFixedReply> respond(
            String message,
            SchoolAssistantContext context,
            List<ChatMessage> recentMessages
    ) {
        SchoolAssistantIntent intent = intentDetector.detectIntent(message);
        LocalDate today = LocalDate.now(clock);
        Optional<LocalDate> selectedPurchaseDate = intentDetector.purchaseDate(message, today);
        Optional<RecessTime> selectedRecessTime = intentDetector.recessTime(message);
        SchoolAssistantFixedTopicRequest request = new SchoolAssistantFixedTopicRequest(
                intent,
                context,
                recentMessages,
                selectedPurchaseDate,
                selectedRecessTime
        );

        return handlers.stream()
                .filter(handler -> handler.supports(request))
                .findFirst()
                .map(handler -> handler.handle(request));
    }

    private List<SchoolAssistantFixedTopicHandler> handlers() {
        return List.of(
                route(this::isForbiddenQuickPurchase, responses::forbiddenQuickPurchaseReply),
                route(request -> isStudentIntent(request, SchoolAssistantIntent.BALANCE),
                        request -> responses.balanceReply(request.context())),
                route(request -> isStudentIntent(request, SchoolAssistantIntent.RECENT_PURCHASES),
                        request -> responses.recentPurchasesReply(request.context())),
                route(request -> isStudentIntent(request,
                                SchoolAssistantIntent.FREQUENT_PURCHASES),
                        request -> responses.frequentPurchasesReply(request.context())),
                route(request -> hasIntent(request, SchoolAssistantIntent.EVENTS),
                        request -> responses.eventsReply(request.context())),
                route(request -> isStudentIntent(request, SchoolAssistantIntent.MENU),
                        request -> responses.menuReply(request.context())),
                route(request -> isStudentIntent(request, SchoolAssistantIntent.PENDING_ORDERS),
                        request -> responses.studentPendingOrdersReply(request.context())),
                route(request -> isStudentIntent(request, SchoolAssistantIntent.WITHDRAWAL_CODE),
                        request -> responses.studentWithdrawalCodeReply(request.context())),
                route(request -> isStudentIntent(request,
                                SchoolAssistantIntent.REPEAT_LAST_PURCHASE),
                        request -> quickPurchaseHandler.repeatLastPurchaseReply(
                                request.context(),
                                request.selectedPurchaseDate()
                        )),
                route(request -> isStudentIntent(request, SchoolAssistantIntent.BUY_USUAL),
                        request -> quickPurchaseHandler.buyUsualPurchaseReply(
                                request.context(),
                                request.selectedPurchaseDate()
                        )),
                route(request -> isStudentIntent(request,
                                SchoolAssistantIntent.CONFIRM_PURCHASE),
                        request -> quickPurchaseHandler.confirmPurchaseReply(
                                request.context(),
                                request.recentMessages()
                        )),
                route(request -> isStudentIntent(request, SchoolAssistantIntent.SELECT_RECESS),
                        request -> quickPurchaseHandler.selectRecessReply(
                                request.context(),
                                request.recentMessages(),
                                request.selectedRecessTime().orElseThrow()
                        )),
                route(this::isStudentPurchaseDateSelection,
                        request -> quickPurchaseHandler.selectPurchaseDateReply(
                                request.context(),
                                request.recentMessages(),
                                request.selectedPurchaseDate().orElseThrow()
                        )),
                route(this::isStudentPurchaseCancellationWithPendingDraft,
                        request -> quickPurchaseHandler.cancelActionReply(
                                request.recentMessages()
                        )),
                route(request -> isStudentIntent(request,
                                SchoolAssistantIntent.CANCEL_PURCHASE),
                        request -> purchaseCancellationHandler.cancelPurchaseReply(
                                request.context(),
                                request.recentMessages()
                        )),
                route(request -> isStudentIntent(request, SchoolAssistantIntent.CANCEL_ACTION),
                        request -> quickPurchaseHandler.cancelActionReply(
                                request.recentMessages()
                        )),
                route(request -> isKiosqueroIntent(request, KIOSQUERO_PENDING_ORDER_INTENTS),
                        request -> responses.kiosqueroPendingOrdersReply(request.context())),
                route(request -> isKiosqueroIntent(request, SchoolAssistantIntent.STOCK),
                        request -> responses.stockReply(request.context())),
                route(request -> isTutorIntent(request, SchoolAssistantIntent.CHILDREN),
                        request -> responses.childrenReply(request.context())),
                route(request -> isTutorIntent(request, SchoolAssistantIntent.BUDGETS),
                        request -> responses.tutorFactsReply(
                                request.context(),
                                SchoolAssistantIntent.BUDGETS,
                                SchoolAssistantActionType.CONSULTA_PRESUPUESTOS,
                                "Presupuestos"
                        )),
                route(request -> isTutorIntent(request, SchoolAssistantIntent.RESTRICTIONS),
                        request -> responses.tutorFactsReply(
                                request.context(),
                                SchoolAssistantIntent.RESTRICTIONS,
                                SchoolAssistantActionType.CONSULTA_RESTRICCIONES,
                                "Restricciones"
                        ))
        );
    }

    private SchoolAssistantFixedTopicHandler route(
            Predicate<SchoolAssistantFixedTopicRequest> predicate,
            Function<SchoolAssistantFixedTopicRequest, SchoolAssistantFixedReply> reply
    ) {
        return new SchoolAssistantFixedTopicRoute(predicate, reply);
    }

    private boolean isForbiddenQuickPurchase(SchoolAssistantFixedTopicRequest request) {
        return request.context().role() != SchoolAssistantRole.ALUMNO
                && FORBIDDEN_QUICK_PURCHASE_INTENTS.contains(request.intent());
    }

    private boolean isStudentIntent(
            SchoolAssistantFixedTopicRequest request,
            SchoolAssistantIntent intent
    ) {
        return hasRole(request, SchoolAssistantRole.ALUMNO) && hasIntent(request, intent);
    }

    private boolean isStudentPurchaseCancellationWithPendingDraft(
            SchoolAssistantFixedTopicRequest request
    ) {
        return isStudentIntent(request, SchoolAssistantIntent.CANCEL_PURCHASE)
                && quickPurchaseHandler.hasPendingPurchase(request.recentMessages());
    }

    private boolean isStudentPurchaseDateSelection(
            SchoolAssistantFixedTopicRequest request
    ) {
        return isStudentIntent(request, SchoolAssistantIntent.SELECT_PURCHASE_DATE)
                && request.selectedPurchaseDate().isPresent();
    }

    private boolean isTutorIntent(
            SchoolAssistantFixedTopicRequest request,
            SchoolAssistantIntent intent
    ) {
        return hasRole(request, SchoolAssistantRole.TUTOR) && hasIntent(request, intent);
    }

    private boolean isKiosqueroIntent(
            SchoolAssistantFixedTopicRequest request,
            SchoolAssistantIntent intent
    ) {
        return hasRole(request, SchoolAssistantRole.KIOSQUERO) && hasIntent(request, intent);
    }

    private boolean isKiosqueroIntent(
            SchoolAssistantFixedTopicRequest request,
            EnumSet<SchoolAssistantIntent> intents
    ) {
        return hasRole(request, SchoolAssistantRole.KIOSQUERO)
                && intents.contains(request.intent());
    }

    private boolean hasRole(
            SchoolAssistantFixedTopicRequest request,
            SchoolAssistantRole role
    ) {
        return request.context().role() == role;
    }

    private boolean hasIntent(
            SchoolAssistantFixedTopicRequest request,
            SchoolAssistantIntent intent
    ) {
        return request.intent() == intent;
    }
}
