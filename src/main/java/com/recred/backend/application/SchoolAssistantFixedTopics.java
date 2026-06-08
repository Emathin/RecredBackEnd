package com.recred.backend.application;

import com.recred.backend.domain.exceptions.InsufficientBalanceException;
import com.recred.backend.domain.exceptions.PickupTimeSlotNotFoundException;
import com.recred.backend.domain.exceptions.PresupuestoExcedidoException;
import com.recred.backend.domain.exceptions.RestriccionHorariaException;
import com.recred.backend.domain.exceptions.RestriccionSaludException;
import com.recred.backend.domain.models.ChatMessage;
import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.RecessTime;
import com.recred.backend.domain.models.SchoolAssistantAction;
import com.recred.backend.domain.models.SchoolAssistantActionStatus;
import com.recred.backend.domain.models.SchoolAssistantActionType;
import com.recred.backend.domain.models.SchoolAssistantContext;
import com.recred.backend.domain.models.SchoolAssistantIntent;
import com.recred.backend.domain.models.SchoolAssistantRequiredInput;
import com.recred.backend.domain.models.SchoolAssistantRole;
import com.recred.backend.domain.models.SchoolCalendarEvent;
import com.recred.backend.domain.repositories.ProductRepository;
import com.recred.backend.domain.repositories.PurchaseRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class SchoolAssistantFixedTopics {

    private static final int RECENT_PURCHASES_TO_SHOW = 3;
    private static final int FREQUENT_PURCHASES_TO_SHOW = 3;
    private static final int PURCHASE_PATTERN_LOOKBACK = 30;
    private static final int MIN_FREQUENT_PURCHASE_OCCURRENCES = 2;
    private static final String ACTION_STATUS_KEY = "estadoAccionChatbot";
    private static final String PENDING_ACTION_KEY = "accionPendienteChatbot";
    private static final String ACTION_TYPE_QUICK_PURCHASE = "COMPRA_RAPIDA";
    private static final String ACTION_STATUS_PENDING = "PENDIENTE";
    private static final String ACTION_STATUS_EXECUTED = "EJECUTADA";
    private static final String ACTION_STATUS_CANCELLED = "CANCELADA";
    private static final String ACTION_STATUS_FAILED = "FALLIDA";

    private final PurchaseRepository purchaseRepository;
    private final ProductRepository productRepository;
    private final CreateAssistantAdvancePurchase createAssistantAdvancePurchase;
    private final Clock clock;
    private final SchoolAssistantReplyFormatter replyFormatter;
    private final SchoolAssistantContentFormatter contentFormatter;
    private final SchoolAssistantTutorFactsFormatter tutorFactsFormatter;
    private final SchoolAssistantPurchaseFailureMessages purchaseFailureMessages;
    private final SchoolAssistantIntentDetector intentDetector;
    private final SchoolAssistantSuggestionFactory suggestionFactory;
    private final SchoolAssistantPendingPurchaseParser pendingPurchaseParser;
    private final SchoolAssistantReplyBuilder replyBuilder;

    public SchoolAssistantFixedTopics(
            PurchaseRepository purchaseRepository,
            ProductRepository productRepository,
            CreateAssistantAdvancePurchase createAssistantAdvancePurchase,
            Clock clock
    ) {
        this.purchaseRepository = purchaseRepository;
        this.productRepository = productRepository;
        this.createAssistantAdvancePurchase = createAssistantAdvancePurchase;
        this.clock = clock;
        this.replyFormatter = new SchoolAssistantReplyFormatter();
        this.contentFormatter = new SchoolAssistantContentFormatter(
                productRepository,
                replyFormatter
        );
        this.tutorFactsFormatter = new SchoolAssistantTutorFactsFormatter();
        this.purchaseFailureMessages = new SchoolAssistantPurchaseFailureMessages();
        this.intentDetector = new SchoolAssistantIntentDetector();
        this.suggestionFactory = new SchoolAssistantSuggestionFactory();
        this.pendingPurchaseParser = new SchoolAssistantPendingPurchaseParser(intentDetector);
        this.replyBuilder = new SchoolAssistantReplyBuilder(suggestionFactory);
    }

    public Optional<SchoolAssistantFixedReply> respond(
            String message,
            SchoolAssistantContext context,
            List<ChatMessage> recentMessages
    ) {
        SchoolAssistantIntent intent = intentDetector.detectIntent(message);
        Optional<RecessTime> selectedRecessTime = intentDetector.recessTime(message);

        if (context.role() != SchoolAssistantRole.ALUMNO) {
            Optional<SchoolAssistantFixedReply> reply = nonStudentReply(intent, context);
            return reply;
        }

        return studentReply(intent, context, recentMessages, selectedRecessTime);
    }

    private Optional<SchoolAssistantFixedReply> studentReply(
            SchoolAssistantIntent intent,
            SchoolAssistantContext context,
            List<ChatMessage> recentMessages,
            Optional<RecessTime> selectedRecessTime
    ) {
        return switch (intent) {
            case BALANCE -> Optional.of(balanceReply(context));
            case RECENT_PURCHASES -> Optional.of(recentPurchasesReply(context));
            case FREQUENT_PURCHASES -> Optional.of(frequentPurchasesReply(context));
            case EVENTS -> Optional.of(eventsReply(context));
            case MENU -> Optional.of(menuReply(context));
            case PENDING_ORDERS -> Optional.of(studentPendingOrdersReply(context));
            case WITHDRAWAL_CODE -> Optional.of(studentWithdrawalCodeReply(context));
            case REPEAT_LAST_PURCHASE -> Optional.of(repeatLastPurchaseReply(context));
            case BUY_USUAL -> Optional.of(buyUsualPurchaseReply(context));
            case CONFIRM_PURCHASE -> Optional.of(confirmPurchaseReply(context, recentMessages));
            case SELECT_RECESS -> Optional.of(selectRecessReply(
                    context,
                    recentMessages,
                    selectedRecessTime.orElseThrow()
            ));
            case CANCEL_ACTION -> Optional.of(cancelActionReply(recentMessages));
            case UNKNOWN -> Optional.empty();
            default -> Optional.empty();
        };
    }

    private Optional<SchoolAssistantFixedReply> nonStudentReply(
            SchoolAssistantIntent intent,
            SchoolAssistantContext context
    ) {
        return switch (intent) {
            case REPEAT_LAST_PURCHASE, BUY_USUAL, CONFIRM_PURCHASE, CANCEL_ACTION ->
                    Optional.of(reply(
                            "Por ahora las compras rapidas desde el asistente "
                                    + "estan disponibles para alumnos.",
                            intent,
                            forbiddenQuickPurchaseAction(),
                            context.role()
                    ));
            case EVENTS -> Optional.of(eventsReply(context));
            case PENDING_ORDERS, ORDER_STATUS, WITHDRAWAL_CODE -> kiosqueroOnly(
                    context,
                    kiosqueroPendingOrdersReply(context)
            );
            case STOCK -> kiosqueroOnly(context, stockReply(context));
            case CHILDREN -> tutorOnly(context, childrenReply(context));
            case BUDGETS -> tutorOnly(context, tutorFactsReply(
                    context,
                    SchoolAssistantIntent.BUDGETS,
                    SchoolAssistantActionType.CONSULTA_PRESUPUESTOS,
                    "Presupuestos"
            ));
            case RESTRICTIONS -> tutorOnly(context, tutorFactsReply(
                    context,
                    SchoolAssistantIntent.RESTRICTIONS,
                    SchoolAssistantActionType.CONSULTA_RESTRICCIONES,
                    "Restricciones"
            ));
            default -> Optional.empty();
        };
    }

    private Optional<SchoolAssistantFixedReply> kiosqueroOnly(
            SchoolAssistantContext context,
            SchoolAssistantFixedReply reply
    ) {
        return context.role() == SchoolAssistantRole.KIOSQUERO
                ? Optional.of(reply)
                : Optional.empty();
    }

    private Optional<SchoolAssistantFixedReply> tutorOnly(
            SchoolAssistantContext context,
            SchoolAssistantFixedReply reply
    ) {
        return context.role() == SchoolAssistantRole.TUTOR
                ? Optional.of(reply)
                : Optional.empty();
    }

    private SchoolAssistantFixedReply kiosqueroPendingOrdersReply(
            SchoolAssistantContext context
    ) {
        List<Purchase> purchases = purchaseRepository.findByBuffetAndStatuses(
                context.buffetId(),
                pendingStatuses(),
                RECENT_PURCHASES_TO_SHOW
        );
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("pedidos", contentFormatter.purchasesData(purchases));
        SchoolAssistantAction action = informativeAction(
                SchoolAssistantActionType.CONSULTA_PEDIDOS,
                data
        );

        return purchases.isEmpty()
                ? reply("No hay pedidos pendientes en el buffet.",
                        SchoolAssistantIntent.PENDING_ORDERS, action, context.role())
                : reply("Pedidos pendientes:\n" + contentFormatter.purchaseList(purchases),
                        SchoolAssistantIntent.PENDING_ORDERS, action, context.role());
    }

    private SchoolAssistantFixedReply stockReply(SchoolAssistantContext context) {
        List<Producto> products = productRepository.findAllActiveWithStock(context.buffetId());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("productos", contentFormatter.productsData(products));
        SchoolAssistantAction action = informativeAction(
                SchoolAssistantActionType.CONSULTA_STOCK,
                data
        );

        return products.isEmpty()
                ? reply("No encontre productos activos con stock.",
                        SchoolAssistantIntent.STOCK, action, context.role())
                : reply("Stock actual (" + replyFormatter.countText(
                        products.size(),
                        "producto",
                        "productos"
                )
                        + "):\n" + contentFormatter.productsList(products),
                        SchoolAssistantIntent.STOCK, action, context.role());
    }

    private SchoolAssistantFixedReply childrenReply(SchoolAssistantContext context) {
        List<String> children = factsByPrefix(context, "hijo=");
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("hijos", children);
        SchoolAssistantAction action = informativeAction(
                SchoolAssistantActionType.CONSULTA_HIJOS,
                data
        );

        List<String> visibleChildren = tutorFactsFormatter.children(children);

        return children.isEmpty()
                ? reply("No encontre hijos asociados al tutor.",
                        SchoolAssistantIntent.CHILDREN, action, context.role())
                : reply("Hijos asociados:\n" + replyFormatter.formattedItems(visibleChildren),
                        SchoolAssistantIntent.CHILDREN, action, context.role());
    }

    private SchoolAssistantFixedReply tutorFactsReply(
            SchoolAssistantContext context,
            SchoolAssistantIntent intent,
            SchoolAssistantActionType actionType,
            String prefix
    ) {
        List<String> children = factsByPrefix(context, "hijo=");
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("hijos", children);
        SchoolAssistantAction action = informativeAction(actionType, data);

        List<String> visibleChildren = tutorFactsFormatter.facts(intent, children);

        return children.isEmpty()
                ? reply("No encontre datos de hijos asociados al tutor.", intent, action,
                        context.role())
                : reply(
                        prefix + ":\n" + replyFormatter.formattedItems(visibleChildren),
                        intent,
                        action,
                        context.role()
                );
    }

    private SchoolAssistantFixedReply balanceReply(SchoolAssistantContext context) {
        String answer = context.balance()
                .map(balance -> "Tenes " + contentFormatter.money(balance) + " disponibles.")
                .orElse("No encontre saldo registrado para este alumno.");
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("saldo", context.balance().orElse(null));
        return reply(
                answer,
                SchoolAssistantIntent.BALANCE,
                informativeAction(SchoolAssistantActionType.CONSULTA_SALDO, data)
        );
    }

    private SchoolAssistantFixedReply recentPurchasesReply(SchoolAssistantContext context) {
        List<Purchase> purchases = purchaseHistory(context.studentId()).stream()
                .limit(RECENT_PURCHASES_TO_SHOW)
                .toList();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("compras", contentFormatter.purchasesData(purchases));

        return purchases.isEmpty()
                ? reply(
                        "No encontre compras registradas para este alumno.",
                        SchoolAssistantIntent.RECENT_PURCHASES,
                        informativeAction(SchoolAssistantActionType.CONSULTA_COMPRAS, data)
                )
                : reply(
                        "Tus ultimas compras:\n" + contentFormatter.purchaseList(purchases),
                        SchoolAssistantIntent.RECENT_PURCHASES,
                        informativeAction(SchoolAssistantActionType.CONSULTA_COMPRAS, data)
                );
    }

    private SchoolAssistantFixedReply frequentPurchasesReply(SchoolAssistantContext context) {
        List<PurchasePattern> patterns = purchasePatterns(context.studentId()).stream()
                .limit(FREQUENT_PURCHASES_TO_SHOW)
                .toList();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("patrones", contentFormatter.patternsData(patterns));

        return patterns.isEmpty()
                ? reply(
                        "Todavia no detecte compras frecuentes para este alumno.",
                        SchoolAssistantIntent.FREQUENT_PURCHASES,
                        informativeAction(
                                SchoolAssistantActionType.CONSULTA_COMPRAS_FRECUENTES,
                                data
                        )
                )
                : reply(
                        "Tus compras frecuentes:\n" + contentFormatter.patternList(patterns),
                        SchoolAssistantIntent.FREQUENT_PURCHASES,
                        informativeAction(
                                SchoolAssistantActionType.CONSULTA_COMPRAS_FRECUENTES,
                                data
                        )
                );
    }

    private SchoolAssistantFixedReply eventsReply(SchoolAssistantContext context) {
        List<SchoolCalendarEvent> events = context.upcomingEvents();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("eventos", contentFormatter.eventsData(events));
        SchoolAssistantAction action = informativeAction(
                SchoolAssistantActionType.CONSULTA_EVENTOS,
                data
        );

        return events.isEmpty()
                ? reply("No encontre eventos escolares proximos.", SchoolAssistantIntent.EVENTS,
                        action, context.role())
                : reply("Eventos proximos:\n" + contentFormatter.eventsList(events),
                        SchoolAssistantIntent.EVENTS, action, context.role());
    }

    private SchoolAssistantFixedReply menuReply(SchoolAssistantContext context) {
        Optional<UUID> buffetId = lastBuffetId(context.studentId());

        if (buffetId.isEmpty()) {
            return reply(
                    "Todavia no tengo un buffet de referencia para mostrar productos.",
                    SchoolAssistantIntent.MENU,
                    informativeAction(SchoolAssistantActionType.CONSULTA_MENU, Map.of())
            );
        }

        List<Producto> products = productRepository.findAllActiveWithStock(buffetId.orElseThrow());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("buffetId", buffetId.orElseThrow().toString());
        data.put("productos", contentFormatter.productsData(products));
        SchoolAssistantAction action = informativeAction(
                SchoolAssistantActionType.CONSULTA_MENU,
                data
        );

        return products.isEmpty()
                ? reply("No encontre productos disponibles para ese buffet.",
                        SchoolAssistantIntent.MENU, action)
                : reply("Productos disponibles ("
                        + replyFormatter.countText(products.size(), "producto", "productos")
                        + "):\n" + contentFormatter.productsList(products),
                        SchoolAssistantIntent.MENU, action);
    }

    private SchoolAssistantFixedReply studentPendingOrdersReply(
            SchoolAssistantContext context
    ) {
        List<Purchase> purchases = pendingStudentPurchases(context.studentId());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("pedidos", contentFormatter.purchasesData(purchases));
        SchoolAssistantAction action = informativeAction(
                SchoolAssistantActionType.CONSULTA_PEDIDOS,
                data
        );

        return purchases.isEmpty()
                ? reply("No tenes pedidos pendientes.", SchoolAssistantIntent.PENDING_ORDERS,
                        action)
                : reply("Tus pedidos pendientes:\n" + contentFormatter.purchaseList(purchases),
                        SchoolAssistantIntent.PENDING_ORDERS, action);
    }

    private SchoolAssistantFixedReply studentWithdrawalCodeReply(
            SchoolAssistantContext context
    ) {
        Optional<Purchase> purchase = pendingStudentPurchases(context.studentId()).stream()
                .filter(this::hasWithdrawalCode)
                .findFirst();
        Map<String, Object> data = new LinkedHashMap<>();
        purchase.ifPresent(value -> data.put("pedido", contentFormatter.purchaseData(value)));
        SchoolAssistantAction action = informativeAction(
                SchoolAssistantActionType.CONSULTA_CODIGO_RETIRO,
                data
        );

        return purchase
                .map(value -> reply(
                        "Tu codigo de retiro es " + value.getWithdrawalCode() + ".",
                        SchoolAssistantIntent.WITHDRAWAL_CODE,
                        action
                ))
                .orElseGet(() -> reply(
                        "No encontre un pedido pendiente con codigo de retiro.",
                        SchoolAssistantIntent.WITHDRAWAL_CODE,
                        action
                ));
    }

    private SchoolAssistantFixedReply repeatLastPurchaseReply(SchoolAssistantContext context) {
        Optional<Purchase> purchase = purchaseHistory(context.studentId()).stream()
                .filter(this::isRebuyablePurchase)
                .findFirst();

        return purchase
                .flatMap(lastPurchase -> planFromPurchase(context.studentId(), lastPurchase, 1))
                .map(plan -> pendingPurchaseReply(
                        plan,
                        SchoolAssistantIntent.REPEAT_LAST_PURCHASE,
                        "Vas a repetir tu ultima compra"
                ))
                .orElseGet(() -> reply(
                        "No encontre una compra entregada disponible para repetir.",
                        SchoolAssistantIntent.REPEAT_LAST_PURCHASE,
                        failedAdvanceAction(Map.of())
                ));
    }

    private SchoolAssistantFixedReply buyUsualPurchaseReply(SchoolAssistantContext context) {
        return mostFrequentPlan(context.studentId())
                .map(plan -> pendingPurchaseReply(
                        plan,
                        SchoolAssistantIntent.BUY_USUAL,
                        "Tu compra habitual detectada"
                ))
                .orElseGet(() -> reply(
                        "Todavia no detecte una compra habitual disponible para repetir.",
                        SchoolAssistantIntent.BUY_USUAL,
                        failedAdvanceAction(Map.of())
                ));
    }

    private SchoolAssistantFixedReply confirmPurchaseReply(
            SchoolAssistantContext context,
            List<ChatMessage> recentMessages
    ) {
        return pendingPurchaseParser.pendingPurchase(recentMessages)
                .map(pendingPurchase -> executePendingPurchase(context, pendingPurchase))
                .orElseGet(() -> reply(
                        "No hay una compra pendiente para confirmar.",
                        SchoolAssistantIntent.CONFIRM_PURCHASE,
                        failedAdvanceAction(Map.of())
                ));
    }

    private SchoolAssistantFixedReply selectRecessReply(
            SchoolAssistantContext context,
            List<ChatMessage> recentMessages,
            RecessTime recessTime
    ) {
        return pendingPurchaseParser.pendingPurchase(recentMessages)
                .map(pendingPurchase -> withSelectedRecessReply(context, pendingPurchase,
                        recessTime))
                .orElseGet(() -> reply(
                        "No hay una compra pendiente para asignarle recreo.",
                        SchoolAssistantIntent.SELECT_RECESS,
                        failedAdvanceAction(Map.of())
                ));
    }

    private SchoolAssistantFixedReply cancelActionReply(List<ChatMessage> recentMessages) {
        boolean hasPendingPurchase = pendingPurchaseParser.pendingPurchase(recentMessages)
                .isPresent();
        String answer = hasPendingPurchase
                ? "Listo, descarte la compra pendiente."
                : "No hay una compra pendiente para cancelar.";
        Map<String, Object> metadata = metadata(SchoolAssistantIntent.CANCEL_ACTION.name());
        metadata.put(ACTION_STATUS_KEY, ACTION_STATUS_CANCELLED);
        return new SchoolAssistantFixedReply(
                answer,
                metadata,
                SchoolAssistantIntent.CANCEL_ACTION,
                advanceAction(
                        SchoolAssistantActionStatus.CANCELADA,
                        false,
                        List.of(),
                        Map.of(),
                        null
                ),
                suggestionFactory.quickPurchaseSuggestions(),
                Map.of()
        );
    }

    private SchoolAssistantFixedReply executePendingPurchase(
            SchoolAssistantContext context,
            PendingPurchase pendingPurchase
    ) {
        UUID studentId = context.studentId();
        if (studentDoesNotOwnPendingPurchase(studentId, pendingPurchase)) {
            SchoolAssistantFixedReply reply = failedPurchaseReply(
                    "La compra pendiente no corresponde a este alumno."
            );
            return reply;
        }

        if (pendingPurchase.recessTime().isEmpty()) {
            return missingRecessReply(pendingPurchase);
        }

        Optional<Map<Producto, Integer>> cart = cart(pendingPurchase);

        if (cart.isEmpty()) {
            SchoolAssistantFixedReply reply = failedPurchaseReply(
                    "No pude realizar la compra porque algun producto ya no esta disponible."
            );
            return reply;
        }

        try {
            Purchase purchase = createAssistantAdvancePurchase.execute(
                    studentId,
                    pendingPurchase.buffetId(),
                    cart.orElseThrow(),
                    pendingPurchase.recessTime().orElseThrow(),
                    LocalDate.now(clock)
            );
            return executedPurchaseReply(purchase, pendingPurchase.recessTime().orElseThrow());
        } catch (InsufficientBalanceException
                 | PickupTimeSlotNotFoundException
                 | PresupuestoExcedidoException
                 | RestriccionHorariaException
                 | RestriccionSaludException exception) {
            return failedPurchaseReply(purchaseFailureMessages.message(exception));
        }
    }

    private SchoolAssistantFixedReply withSelectedRecessReply(
            SchoolAssistantContext context,
            PendingPurchase pendingPurchase,
            RecessTime recessTime
    ) {
        if (studentDoesNotOwnPendingPurchase(context.studentId(), pendingPurchase)) {
            return failedPurchaseReply("La compra pendiente no corresponde a este alumno.");
        }

        PendingPurchase updatedPurchase = new PendingPurchase(
                pendingPurchase.studentId(),
                pendingPurchase.buffetId(),
                pendingPurchase.items(),
                Optional.of(recessTime)
        );
        Map<String, Object> metadata = pendingMetadata(
                SchoolAssistantIntent.SELECT_RECESS.name(),
                updatedPurchase
        );
        Map<String, Object> data = contentFormatter.pendingPurchaseData(updatedPurchase);
        SchoolAssistantAction action = advanceAction(
                SchoolAssistantActionStatus.ESPERANDO_CONFIRMACION,
                true,
                List.of(SchoolAssistantRequiredInput.CONFIRMACION),
                data,
                recessTime
        );

        return new SchoolAssistantFixedReply(
                "Listo, la dejo para " + contentFormatter.recessLabel(recessTime)
                        + ". Escribi confirmar para crear la compra anticipada o cancelar.",
                metadata,
                SchoolAssistantIntent.SELECT_RECESS,
                action,
                suggestionFactory.confirmationSuggestions(),
                data
        );
    }

    private SchoolAssistantFixedReply executedPurchaseReply(
            Purchase purchase,
            RecessTime recessTime
    ) {
        Map<String, Object> metadata = metadata(
                SchoolAssistantIntent.CONFIRM_PURCHASE.name()
        );
        metadata.put(ACTION_STATUS_KEY, ACTION_STATUS_EXECUTED);
        metadata.put("compraId", purchase.getId().toString());
        Map<String, Object> data = contentFormatter.purchaseData(purchase);
        SchoolAssistantAction action = new SchoolAssistantAction(
                SchoolAssistantActionType.COMPRA_ANTICIPADA,
                SchoolAssistantActionStatus.EJECUTADA,
                false,
                List.of(),
                purchase.getId(),
                purchase.getStatus(),
                purchase.getWithdrawalCode(),
                purchase.getTotalAmount(),
                recessTime,
                data
        );

        return new SchoolAssistantFixedReply(
                "Compra anticipada creada. Total: "
                        + contentFormatter.money(purchase.getTotalAmount())
                        + ". Estado: " + purchase.getStatus()
                        + ". Codigo de retiro: "
                        + contentFormatter.value(purchase.getWithdrawalCode()) + ".",
                metadata,
                SchoolAssistantIntent.CONFIRM_PURCHASE,
                action,
                suggestionFactory.quickPurchaseSuggestions(),
                data
        );
    }

    private SchoolAssistantFixedReply missingRecessReply(PendingPurchase pendingPurchase) {
        Map<String, Object> data = contentFormatter.pendingPurchaseData(pendingPurchase);
        return new SchoolAssistantFixedReply(
                "Elegi el recreo de retiro: primer recreo, segundo recreo, mediodia o salida.",
                pendingMetadata(
                        SchoolAssistantIntent.CONFIRM_PURCHASE.name(),
                        pendingPurchase
                ),
                SchoolAssistantIntent.CONFIRM_PURCHASE,
                advanceAction(
                        SchoolAssistantActionStatus.ESPERANDO_RECREO,
                        true,
                        List.of(SchoolAssistantRequiredInput.RECREO),
                        data,
                        null
                ),
                suggestionFactory.recessSuggestions(),
                data
        );
    }

    private SchoolAssistantFixedReply failedPurchaseReply(String answer) {
        Map<String, Object> metadata = metadata(
                SchoolAssistantIntent.CONFIRM_PURCHASE.name()
        );
        metadata.put(ACTION_STATUS_KEY, ACTION_STATUS_FAILED);
        return new SchoolAssistantFixedReply(
                answer,
                metadata,
                SchoolAssistantIntent.CONFIRM_PURCHASE,
                failedAdvanceAction(Map.of()),
                suggestionFactory.quickPurchaseSuggestions(),
                Map.of()
        );
    }

    private SchoolAssistantFixedReply pendingPurchaseReply(
            PurchasePlan plan,
            SchoolAssistantIntent intent,
            String prefix
    ) {
        Map<String, Object> metadata = metadata(intent.name());
        metadata.put(ACTION_STATUS_KEY, ACTION_STATUS_PENDING);
        metadata.put(PENDING_ACTION_KEY, pendingAction(plan));
        Map<String, Object> data = contentFormatter.purchasePlanData(plan);

        return new SchoolAssistantFixedReply(
                prefix + ": " + contentFormatter.itemList(plan.items())
                        + ". Total estimado: " + contentFormatter.money(plan.totalAmount())
                        + ". Elegi el recreo de retiro: primer recreo, segundo recreo, "
                        + "mediodia o salida.",
                metadata,
                intent,
                advanceAction(
                        SchoolAssistantActionStatus.ESPERANDO_RECREO,
                        true,
                        List.of(SchoolAssistantRequiredInput.RECREO),
                        data,
                        null
                ),
                suggestionFactory.recessSuggestions(),
                data
        );
    }

    private Optional<PurchasePlan> mostFrequentPlan(UUID studentId) {
        return purchasePatterns(studentId).stream()
                .map(pattern -> planFromRequests(
                        studentId,
                        pattern.items(),
                        pattern.occurrences(),
                        pattern.lastDate()
                ))
                .flatMap(Optional::stream)
                .findFirst();
    }

    private Optional<PurchasePlan> planFromPurchase(
            UUID studentId,
            Purchase purchase,
            int occurrences
    ) {
        return planFromRequests(
                studentId,
                itemRequests(purchase),
                occurrences,
                purchase.getDate()
        );
    }

    private Optional<PurchasePlan> planFromRequests(
            UUID studentId,
            List<PurchaseItemRequest> requests,
            int occurrences,
            LocalDateTime lastDate
    ) {
        List<PurchasePlanItem> items = requests.stream()
                .map(this::planItem)
                .flatMap(Optional::stream)
                .toList();
        Optional<UUID> buffetId = buffetId(requests);

        return hasCompletePurchasePlan(items, requests, buffetId)
                ? Optional.of(new PurchasePlan(
                        studentId,
                        buffetId.orElseThrow(),
                        items,
                        occurrences,
                        lastDate
                ))
                : Optional.empty();
    }

    private boolean hasCompletePurchasePlan(
            List<PurchasePlanItem> items,
            List<PurchaseItemRequest> requests,
            Optional<UUID> buffetId
    ) {
        return items.size() == requests.size()
                && !items.isEmpty()
                && buffetId.isPresent();
    }

    private Optional<UUID> buffetId(List<PurchaseItemRequest> requests) {
        return requests.stream()
                .map(PurchaseItemRequest::buffetId)
                .filter(Objects::nonNull)
                .findFirst();
    }

    private Optional<PurchasePlanItem> planItem(PurchaseItemRequest request) {
        return productRepository.findActiveById(request.productId())
                .map(product -> new PurchasePlanItem(
                        product.getId(),
                        contentFormatter.productName(product),
                        request.quantity(),
                        contentFormatter.price(product)
                ));
    }

    private Optional<Map<Producto, Integer>> cart(PendingPurchase pendingPurchase) {
        Map<Producto, Integer> cart = new LinkedHashMap<>();
        pendingPurchase.items().forEach(item -> productRepository.findActiveById(item.productId())
                .ifPresent(product -> cart.put(product, item.quantity())));

        return cart.size() == pendingPurchase.items().size()
                ? Optional.of(cart)
                : Optional.empty();
    }

    private List<PurchasePattern> purchasePatterns(UUID studentId) {
        return purchaseHistory(studentId).stream()
                .filter(this::isRebuyablePurchase)
                .limit(PURCHASE_PATTERN_LOOKBACK)
                .map(this::patternCandidate)
                .filter(candidate -> !candidate.items().isEmpty())
                .collect(Collectors.toMap(
                        PurchasePatternCandidate::key,
                        PurchasePatternAccumulator::new,
                        PurchasePatternAccumulator::merge,
                        LinkedHashMap::new
                ))
                .values()
                .stream()
                .map(PurchasePatternAccumulator::pattern)
                .filter(this::hasMinimumOccurrences)
                .sorted(this::comparePatterns)
                .toList();
    }

    private PurchasePatternCandidate patternCandidate(Purchase purchase) {
        List<PurchaseItemRequest> items = itemRequests(purchase);
        String key = items.stream()
                .map(item -> item.productId() + ":" + item.quantity())
                .collect(Collectors.joining("|"));
        return new PurchasePatternCandidate(key, items, purchase.getDate());
    }

    private List<PurchaseItemRequest> itemRequests(Purchase purchase) {
        return purchase.getItems().entrySet()
                .stream()
                .filter(entry -> isValidPurchaseItem(entry.getKey(), entry.getValue()))
                .map(entry -> new PurchaseItemRequest(
                        entry.getKey().getId(),
                        entry.getValue(),
                        purchase.getBuffetId()
                ))
                .sorted(Comparator.comparing(item -> item.productId().toString()))
                .toList();
    }

    private List<Purchase> purchaseHistory(UUID studentId) {
        return purchaseRepository.findHistorialByAlumno(studentId);
    }

    private Map<String, Object> pendingAction(PurchasePlan plan) {
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("tipo", ACTION_TYPE_QUICK_PURCHASE);
        action.put("alumnoId", plan.studentId().toString());
        action.put("buffetId", plan.buffetId().toString());
        action.put("items", plan.items().stream()
                .map(this::pendingActionItem)
                .toList());
        return action;
    }

    private Map<String, Object> pendingAction(PendingPurchase pendingPurchase) {
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("tipo", ACTION_TYPE_QUICK_PURCHASE);
        action.put("alumnoId", pendingPurchase.studentId().toString());
        action.put("buffetId", pendingPurchase.buffetId().toString());
        pendingPurchase.recessTime().ifPresent(recessTime ->
                action.put("recreo", recessTime.name()));
        action.put("items", pendingPurchase.items().stream()
                .map(this::pendingActionItem)
                .toList());
        return action;
    }

    private Map<String, Object> pendingActionItem(PurchaseItemRequest item) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("productoId", item.productId().toString());
        metadata.put("cantidad", item.quantity());
        return metadata;
    }

    private Map<String, Object> pendingActionItem(PurchasePlanItem item) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("productoId", item.productId().toString());
        metadata.put("cantidad", item.quantity());
        return metadata;
    }

    private Map<String, Object> metadata(String intent) {
        return replyBuilder.metadata(intent);
    }

    private Map<String, Object> pendingMetadata(String intent, PendingPurchase pendingPurchase) {
        Map<String, Object> metadata = metadata(intent);
        metadata.put(ACTION_STATUS_KEY, ACTION_STATUS_PENDING);
        metadata.put(PENDING_ACTION_KEY, pendingAction(pendingPurchase));
        return metadata;
    }

    private SchoolAssistantFixedReply reply(
            String answer,
            SchoolAssistantIntent intent,
            SchoolAssistantAction action
    ) {
        return replyBuilder.reply(answer, intent, action);
    }

    private SchoolAssistantFixedReply reply(
            String answer,
            SchoolAssistantIntent intent,
            SchoolAssistantAction action,
            SchoolAssistantRole role
    ) {
        return replyBuilder.reply(answer, intent, action, role);
    }

    private SchoolAssistantAction informativeAction(
            SchoolAssistantActionType type,
            Map<String, Object> data
    ) {
        return replyBuilder.informativeAction(type, data);
    }

    private SchoolAssistantAction forbiddenQuickPurchaseAction() {
        return replyBuilder.forbiddenQuickPurchaseAction();
    }

    private SchoolAssistantAction failedAdvanceAction(Map<String, Object> data) {
        return replyBuilder.failedAdvanceAction(data);
    }

    private SchoolAssistantAction advanceAction(
            SchoolAssistantActionStatus status,
            boolean requiresInput,
            List<SchoolAssistantRequiredInput> requiredInputs,
            Map<String, Object> data,
            RecessTime recessTime
    ) {
        return replyBuilder.advanceAction(
                status,
                requiresInput,
                requiredInputs,
                data,
                recessTime
        );
    }

    private boolean isRebuyablePurchase(Purchase purchase) {
        return purchase != null
                && purchase.getStatus() == EstadoCompra.ENTREGADO
                && purchase.getItems() != null
                && !purchase.getItems().isEmpty();
    }

    private boolean isValidPurchaseItem(Producto product, Integer quantity) {
        return product != null
                && product.getId() != null
                && quantity != null
                && quantity > 0;
    }

    private boolean hasMinimumOccurrences(PurchasePattern pattern) {
        return pattern.occurrences() >= MIN_FREQUENT_PURCHASE_OCCURRENCES;
    }

    private int comparePatterns(PurchasePattern first, PurchasePattern second) {
        int occurrencesComparison = Integer.compare(second.occurrences(), first.occurrences());
        return occurrencesComparison != 0
                ? occurrencesComparison
                : compareDates(first.lastDate(), second.lastDate());
    }

    private int compareDates(LocalDateTime first, LocalDateTime second) {
        return Comparator.nullsLast(LocalDateTime::compareTo)
                .reversed()
                .compare(first, second);
    }

    private boolean studentDoesNotOwnPendingPurchase(
            UUID studentId,
            PendingPurchase pendingPurchase
    ) {
        return !pendingPurchase.studentId().equals(studentId);
    }

    private List<EstadoCompra> pendingStatuses() {
        return List.of(
                EstadoCompra.PENDIENTE,
                EstadoCompra.EN_PREPARACION,
                EstadoCompra.LISTO
        );
    }

    private List<Purchase> pendingStudentPurchases(UUID studentId) {
        return purchaseHistory(studentId).stream()
                .filter(this::isPendingPurchase)
                .limit(RECENT_PURCHASES_TO_SHOW)
                .toList();
    }

    private boolean isPendingPurchase(Purchase purchase) {
        return purchase != null && pendingStatuses().contains(purchase.getStatus());
    }

    private boolean hasWithdrawalCode(Purchase purchase) {
        return purchase.getWithdrawalCode() != null && !purchase.getWithdrawalCode().isBlank();
    }

    private Optional<UUID> lastBuffetId(UUID studentId) {
        return purchaseHistory(studentId).stream()
                .map(Purchase::getBuffetId)
                .filter(Objects::nonNull)
                .findFirst();
    }

    private List<String> factsByPrefix(SchoolAssistantContext context, String prefix) {
        return context.facts().stream()
                .filter(fact -> fact.startsWith(prefix))
                .map(fact -> fact.substring(prefix.length()))
                .toList();
    }

}
