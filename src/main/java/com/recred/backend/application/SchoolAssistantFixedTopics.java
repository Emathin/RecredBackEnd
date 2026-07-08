package com.recred.backend.application;

import com.recred.backend.domain.models.ChatMessage;
import com.recred.backend.domain.models.SchoolAssistantActionType;
import com.recred.backend.domain.models.SchoolAssistantContext;
import com.recred.backend.domain.models.SchoolAssistantIntent;
import com.recred.backend.domain.models.SchoolAssistantRole;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

public class SchoolAssistantFixedTopics {

    private static final EnumSet<SchoolAssistantIntent> KIOSQUERO_PENDING_ORDER_INTENTS =
            EnumSet.of(
                    SchoolAssistantIntent.PENDING_ORDERS,
                    SchoolAssistantIntent.ORDER_STATUS,
                    SchoolAssistantIntent.WITHDRAWAL_CODE
            );

    private final SchoolAssistantIntentDetector intentDetector;
    private final SchoolAssistantFixedTopicResponses responses;
    private final List<SchoolAssistantFixedTopicHandler> handlers;

    public SchoolAssistantFixedTopics(
            SchoolAssistantIntentDetector intentDetector,
            SchoolAssistantFixedTopicResponses responses
    ) {
        this.intentDetector = intentDetector;
        this.responses = responses;
        this.handlers = handlers();
    }

    public Optional<SchoolAssistantFixedReply> respond(
            String message,
            SchoolAssistantContext context,
            List<ChatMessage> recentMessages
    ) {
        SchoolAssistantIntent intent = intentDetector.detectIntent(message);
        SchoolAssistantFixedTopicRequest request = new SchoolAssistantFixedTopicRequest(
                intent,
                context,
                recentMessages,
                Optional.empty(),
                Optional.empty()
        );

        return handlers.stream()
                .filter(handler -> handler.supports(request))
                .findFirst()
                .map(handler -> handler.handle(request));
    }

    private List<SchoolAssistantFixedTopicHandler> handlers() {
        return List.of(
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

    private boolean isStudentIntent(
            SchoolAssistantFixedTopicRequest request,
            SchoolAssistantIntent intent
    ) {
        return hasRole(request, SchoolAssistantRole.ALUMNO) && hasIntent(request, intent);
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
