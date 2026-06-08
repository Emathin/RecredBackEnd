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
import com.recred.backend.domain.models.RolChatMensaje;
import com.recred.backend.domain.models.SchoolAssistantAction;
import com.recred.backend.domain.models.SchoolAssistantActionStatus;
import com.recred.backend.domain.models.SchoolAssistantActionType;
import com.recred.backend.domain.models.SchoolAssistantContext;
import com.recred.backend.domain.models.SchoolAssistantIntent;
import com.recred.backend.domain.models.SchoolAssistantRequiredInput;
import com.recred.backend.domain.models.SchoolAssistantRole;
import com.recred.backend.domain.models.SchoolAssistantSuggestion;
import com.recred.backend.domain.models.SchoolCalendarEvent;
import com.recred.backend.domain.repositories.ProductRepository;
import com.recred.backend.domain.repositories.PurchaseRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SchoolAssistantFixedTopics {

    private static final int RECENT_PURCHASES_TO_SHOW = 3;
    private static final int FREQUENT_PURCHASES_TO_SHOW = 3;
    private static final int PURCHASE_PATTERN_LOOKBACK = 30;
    private static final int MIN_FREQUENT_PURCHASE_OCCURRENCES = 2;
    private static final String INTENT_KEY = "intencionChatbot";
    private static final String ACTION_STATUS_KEY = "estadoAccionChatbot";
    private static final String PENDING_ACTION_KEY = "accionPendienteChatbot";
    private static final String ACTION_TYPE_QUICK_PURCHASE = "COMPRA_RAPIDA";
    private static final String ACTION_STATUS_PENDING = "PENDIENTE";
    private static final String ACTION_STATUS_EXECUTED = "EJECUTADA";
    private static final String ACTION_STATUS_CANCELLED = "CANCELADA";
    private static final String ACTION_STATUS_FAILED = "FALLIDA";
    private static final String PRODUCT_WITHOUT_NAME = "Producto";
    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");
    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final PurchaseRepository purchaseRepository;
    private final ProductRepository productRepository;
    private final CreateAssistantAdvancePurchase createAssistantAdvancePurchase;
    private final Clock clock;

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
    }

    public Optional<SchoolAssistantFixedReply> respond(
            String message,
            SchoolAssistantContext context,
            List<ChatMessage> recentMessages
    ) {
        SchoolAssistantIntent intent = detectIntent(message);
        Optional<RecessTime> selectedRecessTime = recessTime(message);

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
                    "Presupuestos detectados"
            ));
            case RESTRICTIONS -> tutorOnly(context, tutorFactsReply(
                    context,
                    SchoolAssistantIntent.RESTRICTIONS,
                    SchoolAssistantActionType.CONSULTA_RESTRICCIONES,
                    "Restricciones detectadas"
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
        data.put("pedidos", purchasesData(purchases));
        SchoolAssistantAction action = informativeAction(
                SchoolAssistantActionType.CONSULTA_PEDIDOS,
                data
        );

        return purchases.isEmpty()
                ? reply("No hay pedidos pendientes en el buffet.",
                        SchoolAssistantIntent.PENDING_ORDERS, action, context.role())
                : reply("Pedidos pendientes: " + purchaseList(purchases) + ".",
                        SchoolAssistantIntent.PENDING_ORDERS, action, context.role());
    }

    private SchoolAssistantFixedReply stockReply(SchoolAssistantContext context) {
        List<Producto> products = productRepository.findAllActiveWithStock(context.buffetId());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("productos", productsData(products));
        SchoolAssistantAction action = informativeAction(
                SchoolAssistantActionType.CONSULTA_STOCK,
                data
        );

        return products.isEmpty()
                ? reply("No encontre productos activos con stock.",
                        SchoolAssistantIntent.STOCK, action, context.role())
                : reply("Stock actual: " + productsList(products) + ".",
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

        return children.isEmpty()
                ? reply("No encontre hijos asociados al tutor.",
                        SchoolAssistantIntent.CHILDREN, action, context.role())
                : reply("Hijos asociados: " + String.join("; ", children) + ".",
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

        return children.isEmpty()
                ? reply("No encontre datos de hijos asociados al tutor.", intent, action,
                        context.role())
                : reply(prefix + ": " + String.join("; ", children) + ".", intent, action,
                        context.role());
    }

    private SchoolAssistantFixedReply balanceReply(SchoolAssistantContext context) {
        String answer = context.balance()
                .map(balance -> "Tenes " + money(balance) + " disponibles.")
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
        data.put("compras", purchasesData(purchases));

        return purchases.isEmpty()
                ? reply(
                        "No encontre compras registradas para este alumno.",
                        SchoolAssistantIntent.RECENT_PURCHASES,
                        informativeAction(SchoolAssistantActionType.CONSULTA_COMPRAS, data)
                )
                : reply(
                        "Tus ultimas compras son: " + purchaseList(purchases) + ".",
                        SchoolAssistantIntent.RECENT_PURCHASES,
                        informativeAction(SchoolAssistantActionType.CONSULTA_COMPRAS, data)
                );
    }

    private SchoolAssistantFixedReply frequentPurchasesReply(SchoolAssistantContext context) {
        List<PurchasePattern> patterns = purchasePatterns(context.studentId()).stream()
                .limit(FREQUENT_PURCHASES_TO_SHOW)
                .toList();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("patrones", patternsData(patterns));

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
                        "Tus compras frecuentes son: " + patternList(patterns) + ".",
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
        data.put("eventos", eventsData(events));
        SchoolAssistantAction action = informativeAction(
                SchoolAssistantActionType.CONSULTA_EVENTOS,
                data
        );

        return events.isEmpty()
                ? reply("No encontre eventos escolares proximos.", SchoolAssistantIntent.EVENTS,
                        action, context.role())
                : reply("Eventos proximos: " + eventsList(events) + ".",
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
        data.put("productos", productsData(products));
        SchoolAssistantAction action = informativeAction(
                SchoolAssistantActionType.CONSULTA_MENU,
                data
        );

        return products.isEmpty()
                ? reply("No encontre productos disponibles para ese buffet.",
                        SchoolAssistantIntent.MENU, action)
                : reply("Productos disponibles: " + productsList(products) + ".",
                        SchoolAssistantIntent.MENU, action);
    }

    private SchoolAssistantFixedReply studentPendingOrdersReply(
            SchoolAssistantContext context
    ) {
        List<Purchase> purchases = pendingStudentPurchases(context.studentId());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("pedidos", purchasesData(purchases));
        SchoolAssistantAction action = informativeAction(
                SchoolAssistantActionType.CONSULTA_PEDIDOS,
                data
        );

        return purchases.isEmpty()
                ? reply("No tenes pedidos pendientes.", SchoolAssistantIntent.PENDING_ORDERS,
                        action)
                : reply("Tus pedidos pendientes son: " + purchaseList(purchases) + ".",
                        SchoolAssistantIntent.PENDING_ORDERS, action);
    }

    private SchoolAssistantFixedReply studentWithdrawalCodeReply(
            SchoolAssistantContext context
    ) {
        Optional<Purchase> purchase = pendingStudentPurchases(context.studentId()).stream()
                .filter(this::hasWithdrawalCode)
                .findFirst();
        Map<String, Object> data = new LinkedHashMap<>();
        purchase.ifPresent(value -> data.put("pedido", purchaseData(value)));
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
        return pendingPurchase(recentMessages)
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
        return pendingPurchase(recentMessages)
                .map(pendingPurchase -> withSelectedRecessReply(context, pendingPurchase,
                        recessTime))
                .orElseGet(() -> reply(
                        "No hay una compra pendiente para asignarle recreo.",
                        SchoolAssistantIntent.SELECT_RECESS,
                        failedAdvanceAction(Map.of())
                ));
    }

    private SchoolAssistantFixedReply cancelActionReply(List<ChatMessage> recentMessages) {
        boolean hasPendingPurchase = pendingPurchase(recentMessages).isPresent();
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
                quickPurchaseSuggestions(),
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
            return failedPurchaseReply("No pude realizar la compra: " + exception.getMessage());
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
        Map<String, Object> data = pendingPurchaseData(updatedPurchase);
        SchoolAssistantAction action = advanceAction(
                SchoolAssistantActionStatus.ESPERANDO_CONFIRMACION,
                true,
                List.of(SchoolAssistantRequiredInput.CONFIRMACION),
                data,
                recessTime
        );

        return new SchoolAssistantFixedReply(
                "Listo, la dejo para " + recessLabel(recessTime)
                        + ". Escribi confirmar para crear la compra anticipada o cancelar.",
                metadata,
                SchoolAssistantIntent.SELECT_RECESS,
                action,
                confirmationSuggestions(),
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
        Map<String, Object> data = purchaseData(purchase);
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
                "Compra anticipada creada. Total: " + money(purchase.getTotalAmount())
                        + ". Estado: " + purchase.getStatus()
                        + ". Codigo de retiro: " + value(purchase.getWithdrawalCode()) + ".",
                metadata,
                SchoolAssistantIntent.CONFIRM_PURCHASE,
                action,
                quickPurchaseSuggestions(),
                data
        );
    }

    private SchoolAssistantFixedReply missingRecessReply(PendingPurchase pendingPurchase) {
        Map<String, Object> data = pendingPurchaseData(pendingPurchase);
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
                recessSuggestions(),
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
                quickPurchaseSuggestions(),
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
        Map<String, Object> data = purchasePlanData(plan);

        return new SchoolAssistantFixedReply(
                prefix + ": " + itemList(plan.items())
                        + ". Total estimado: " + money(plan.totalAmount())
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
                recessSuggestions(),
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
                        productName(product),
                        request.quantity(),
                        price(product)
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

    private Optional<PendingPurchase> pendingPurchase(List<ChatMessage> recentMessages) {
        return recentMessages.stream()
                .filter(this::isAssistantMessage)
                .map(ChatMessage::metadata)
                .filter(metadata -> metadata != null && !metadata.isEmpty())
                .map(this::pendingPurchaseFromMetadata)
                .filter(PendingActionSearch::shouldStop)
                .findFirst()
                .flatMap(PendingActionSearch::pendingPurchase);
    }

    private PendingActionSearch pendingPurchaseFromMetadata(Map<String, Object> metadata) {
        String status = value(metadata.get(ACTION_STATUS_KEY));

        if (isTerminalActionStatus(status)) {
            PendingActionSearch search = PendingActionSearch.terminal();
            return search;
        }

        Optional<PendingPurchase> pendingPurchase = pendingAction(metadata)
                .filter(this::isQuickPurchaseAction)
                .flatMap(this::pendingPurchaseFromAction);
        return PendingActionSearch.from(pendingPurchase);
    }

    private Optional<Map<String, Object>> pendingAction(Map<String, Object> metadata) {
        Object action = metadata.get(PENDING_ACTION_KEY);
        return action instanceof Map<?, ?> actionMap
                ? Optional.of(castMap(actionMap))
                : Optional.empty();
    }

    private Optional<PendingPurchase> pendingPurchaseFromAction(Map<String, Object> action) {
        Optional<UUID> studentId = uuid(action.get("alumnoId"));
        Optional<UUID> buffetId = uuid(action.get("buffetId"));
        List<PurchaseItemRequest> items = itemsFromAction(action.get("items"));

        return studentId.isPresent() && buffetId.isPresent() && !items.isEmpty()
                ? Optional.of(new PendingPurchase(
                        studentId.orElseThrow(),
                        buffetId.orElseThrow(),
                        items,
                        recessTime(value(action.get("recreo")))
                ))
                : Optional.empty();
    }

    private List<PurchaseItemRequest> itemsFromAction(Object value) {
        return value instanceof List<?> items
                ? items.stream()
                        .filter(item -> item instanceof Map<?, ?>)
                        .map(item -> castMap((Map<?, ?>) item))
                        .map(this::itemFromAction)
                        .flatMap(Optional::stream)
                        .toList()
                : List.of();
    }

    private Optional<PurchaseItemRequest> itemFromAction(Map<String, Object> item) {
        Optional<UUID> productId = uuid(item.get("productoId"));
        Optional<Integer> quantity = integer(item.get("cantidad"));

        return productId.isPresent() && quantity.filter(value -> value > 0).isPresent()
                ? Optional.of(new PurchaseItemRequest(
                        productId.orElseThrow(),
                        quantity.orElseThrow(),
                        null
                ))
                : Optional.empty();
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
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put(INTENT_KEY, intent);
        return metadata;
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
        return reply(answer, intent, action, SchoolAssistantRole.ALUMNO);
    }

    private SchoolAssistantFixedReply reply(
            String answer,
            SchoolAssistantIntent intent,
            SchoolAssistantAction action,
            SchoolAssistantRole role
    ) {
        return new SchoolAssistantFixedReply(
                answer,
                metadata(intent.name()),
                intent,
                action,
                suggestions(role, intent, action),
                action == null ? Map.of() : action.data()
        );
    }

    private SchoolAssistantAction informativeAction(
            SchoolAssistantActionType type,
            Map<String, Object> data
    ) {
        return SchoolAssistantAction.informative(type, data);
    }

    private SchoolAssistantAction forbiddenQuickPurchaseAction() {
        return advanceAction(
                SchoolAssistantActionStatus.NO_PERMITIDA,
                false,
                List.of(),
                Map.of(),
                null
        );
    }

    private SchoolAssistantAction failedAdvanceAction(Map<String, Object> data) {
        return advanceAction(
                SchoolAssistantActionStatus.FALLIDA,
                false,
                List.of(),
                data,
                null
        );
    }

    private SchoolAssistantAction advanceAction(
            SchoolAssistantActionStatus status,
            boolean requiresInput,
            List<SchoolAssistantRequiredInput> requiredInputs,
            Map<String, Object> data,
            RecessTime recessTime
    ) {
        return new SchoolAssistantAction(
                SchoolAssistantActionType.COMPRA_ANTICIPADA,
                status,
                requiresInput,
                requiredInputs,
                null,
                null,
                null,
                null,
                recessTime,
                data
        );
    }

    private List<SchoolAssistantSuggestion> suggestions(
            SchoolAssistantRole role,
            SchoolAssistantIntent intent,
            SchoolAssistantAction action
    ) {
        SchoolAssistantRole safeRole = role == null ? SchoolAssistantRole.ALUMNO : role;
        List<SchoolAssistantSuggestion> rawSuggestions = rawSuggestions(
                safeRole,
                intent,
                action
        );

        return rawSuggestions.stream()
                .filter(suggestion -> isSuggestionAllowed(safeRole, suggestion))
                .toList();
    }

    private List<SchoolAssistantSuggestion> rawSuggestions(
            SchoolAssistantRole role,
            SchoolAssistantIntent intent,
            SchoolAssistantAction action
    ) {
        return isAdvancePurchaseAction(action)
                ? purchaseSuggestions(role, action)
                : switch (role) {
            case ALUMNO -> studentSuggestions(intent);
            case TUTOR -> tutorSuggestions(intent);
            case KIOSQUERO -> kiosqueroSuggestions(intent);
        };
    }

    private List<SchoolAssistantSuggestion> studentSuggestions(SchoolAssistantIntent intent) {
        return switch (intent) {
            case BALANCE -> List.of(
                    suggestion("Ultimas compras", "mostrame mis ultimas compras",
                            SchoolAssistantActionType.CONSULTA_COMPRAS),
                    suggestion("Compra habitual", "comprame lo de siempre",
                            SchoolAssistantActionType.COMPRA_ANTICIPADA)
            );
            case RECENT_PURCHASES, FREQUENT_PURCHASES -> quickPurchaseSuggestions();
            case EVENTS -> List.of(
                    suggestion("Saldo", "cuanto saldo tengo",
                            SchoolAssistantActionType.CONSULTA_SALDO),
                    suggestion("Compras", "mostrame mis ultimas compras",
                            SchoolAssistantActionType.CONSULTA_COMPRAS)
            );
            case PENDING_ORDERS, ORDER_STATUS, WITHDRAWAL_CODE -> List.of(
                    suggestion("Ultimas compras", "mostrame mis ultimas compras",
                            SchoolAssistantActionType.CONSULTA_COMPRAS),
                    suggestion("Compra habitual", "comprame lo de siempre",
                            SchoolAssistantActionType.COMPRA_ANTICIPADA)
            );
            default -> List.of();
        };
    }

    private List<SchoolAssistantSuggestion> purchaseSuggestions(
            SchoolAssistantRole role,
            SchoolAssistantAction action
    ) {
        return role == SchoolAssistantRole.ALUMNO
                ? switch (action.status()) {
            case ESPERANDO_RECREO -> recessSuggestions();
            case ESPERANDO_CONFIRMACION -> confirmationSuggestions();
            default -> quickPurchaseSuggestions();
        }
                : defaultSuggestions(role);
    }

    private boolean isAdvancePurchaseAction(SchoolAssistantAction action) {
        return action != null && action.type() == SchoolAssistantActionType.COMPRA_ANTICIPADA;
    }

    private List<SchoolAssistantSuggestion> tutorSuggestions(SchoolAssistantIntent intent) {
        return switch (intent) {
            case EVENTS -> List.of(
                    suggestion("Mis hijos", "mostrame mis hijos",
                            SchoolAssistantActionType.CONSULTA_HIJOS),
                    suggestion("Presupuestos", "ver presupuestos de mis hijos",
                            SchoolAssistantActionType.CONSULTA_PRESUPUESTOS),
                    suggestion("Restricciones", "ver restricciones de mis hijos",
                            SchoolAssistantActionType.CONSULTA_RESTRICCIONES)
            );
            case CHILDREN -> List.of(
                    suggestion("Presupuestos", "ver presupuestos de mis hijos",
                            SchoolAssistantActionType.CONSULTA_PRESUPUESTOS),
                    suggestion("Restricciones", "ver restricciones de mis hijos",
                            SchoolAssistantActionType.CONSULTA_RESTRICCIONES),
                    suggestion("Eventos", "que eventos escolares tienen mis hijos",
                            SchoolAssistantActionType.CONSULTA_EVENTOS)
            );
            case BUDGETS, RESTRICTIONS -> List.of(
                    suggestion("Mis hijos", "mostrame mis hijos",
                            SchoolAssistantActionType.CONSULTA_HIJOS),
                    suggestion("Eventos", "que eventos escolares tienen mis hijos",
                            SchoolAssistantActionType.CONSULTA_EVENTOS)
            );
            default -> defaultSuggestions(SchoolAssistantRole.TUTOR);
        };
    }

    private List<SchoolAssistantSuggestion> kiosqueroSuggestions(SchoolAssistantIntent intent) {
        return switch (intent) {
            case STOCK -> List.of(
                    suggestion("Pedidos", "ver pedidos pendientes",
                            SchoolAssistantActionType.CONSULTA_PEDIDOS),
                    suggestion("Ventas", "ventas de hoy",
                            SchoolAssistantActionType.CONSULTA_VENTAS)
            );
            case PENDING_ORDERS, ORDER_STATUS, WITHDRAWAL_CODE -> List.of(
                    suggestion("Stock", "ver stock",
                            SchoolAssistantActionType.CONSULTA_STOCK),
                    suggestion("Ventas", "ventas de hoy",
                            SchoolAssistantActionType.CONSULTA_VENTAS)
            );
            default -> defaultSuggestions(SchoolAssistantRole.KIOSQUERO);
        };
    }

    private List<SchoolAssistantSuggestion> defaultSuggestions(SchoolAssistantRole role) {
        return switch (role) {
            case ALUMNO -> List.of(
                    suggestion("Saldo", "cuanto saldo tengo",
                            SchoolAssistantActionType.CONSULTA_SALDO),
                    suggestion("Ultimas compras", "mostrame mis ultimas compras",
                            SchoolAssistantActionType.CONSULTA_COMPRAS),
                    suggestion("Compra habitual", "comprame lo de siempre",
                            SchoolAssistantActionType.COMPRA_ANTICIPADA),
                    suggestion("Eventos", "que eventos escolares tengo",
                            SchoolAssistantActionType.CONSULTA_EVENTOS)
            );
            case TUTOR -> List.of(
                    suggestion("Mis hijos", "mostrame mis hijos",
                            SchoolAssistantActionType.CONSULTA_HIJOS),
                    suggestion("Presupuestos", "ver presupuestos de mis hijos",
                            SchoolAssistantActionType.CONSULTA_PRESUPUESTOS),
                    suggestion("Restricciones", "ver restricciones de mis hijos",
                            SchoolAssistantActionType.CONSULTA_RESTRICCIONES),
                    suggestion("Eventos", "que eventos escolares tienen mis hijos",
                            SchoolAssistantActionType.CONSULTA_EVENTOS)
            );
            case KIOSQUERO -> List.of(
                    suggestion("Pedidos", "ver pedidos pendientes",
                            SchoolAssistantActionType.CONSULTA_PEDIDOS),
                    suggestion("Stock", "ver stock",
                            SchoolAssistantActionType.CONSULTA_STOCK),
                    suggestion("Ventas", "ventas de hoy",
                            SchoolAssistantActionType.CONSULTA_VENTAS)
            );
        };
    }

    private boolean isSuggestionAllowed(
            SchoolAssistantRole role,
            SchoolAssistantSuggestion suggestion
    ) {
        return switch (role) {
            case ALUMNO -> isStudentSuggestionAllowed(suggestion);
            case TUTOR -> isTutorSuggestionAllowed(suggestion);
            case KIOSQUERO -> isKiosqueroSuggestionAllowed(suggestion);
        };
    }

    private boolean isStudentSuggestionAllowed(SchoolAssistantSuggestion suggestion) {
        return List.of(
                SchoolAssistantActionType.CONSULTA_SALDO,
                SchoolAssistantActionType.CONSULTA_COMPRAS,
                SchoolAssistantActionType.CONSULTA_COMPRAS_FRECUENTES,
                SchoolAssistantActionType.COMPRA_ANTICIPADA,
                SchoolAssistantActionType.CONSULTA_EVENTOS,
                SchoolAssistantActionType.CONSULTA_MENU,
                SchoolAssistantActionType.CONSULTA_PEDIDOS,
                SchoolAssistantActionType.CONSULTA_CODIGO_RETIRO
        ).contains(suggestion.actionType());
    }

    private boolean isTutorSuggestionAllowed(SchoolAssistantSuggestion suggestion) {
        return List.of(
                SchoolAssistantActionType.CONSULTA_HIJOS,
                SchoolAssistantActionType.CONSULTA_PRESUPUESTOS,
                SchoolAssistantActionType.CONSULTA_RESTRICCIONES,
                SchoolAssistantActionType.CONSULTA_EVENTOS,
                SchoolAssistantActionType.CONSULTA_SALDO,
                SchoolAssistantActionType.CONSULTA_COMPRAS
        ).contains(suggestion.actionType());
    }

    private boolean isKiosqueroSuggestionAllowed(SchoolAssistantSuggestion suggestion) {
        return List.of(
                SchoolAssistantActionType.CONSULTA_PEDIDOS,
                SchoolAssistantActionType.CONSULTA_STOCK,
                SchoolAssistantActionType.CONSULTA_VENTAS,
                SchoolAssistantActionType.CONSULTA_COMPRAS
        ).contains(suggestion.actionType());
    }

    private List<SchoolAssistantSuggestion> recessSuggestions() {
        return List.of(
                suggestion("Primer recreo", "primer recreo",
                        SchoolAssistantActionType.COMPRA_ANTICIPADA),
                suggestion("Segundo recreo", "segundo recreo",
                        SchoolAssistantActionType.COMPRA_ANTICIPADA),
                suggestion("Mediodia", "mediodia",
                        SchoolAssistantActionType.COMPRA_ANTICIPADA),
                suggestion("Salida", "salida",
                        SchoolAssistantActionType.COMPRA_ANTICIPADA)
        );
    }

    private List<SchoolAssistantSuggestion> confirmationSuggestions() {
        return List.of(
                suggestion("Confirmar", "confirmar",
                        SchoolAssistantActionType.COMPRA_ANTICIPADA),
                suggestion("Cancelar", "cancelar",
                        SchoolAssistantActionType.COMPRA_ANTICIPADA)
        );
    }

    private List<SchoolAssistantSuggestion> quickPurchaseSuggestions() {
        return List.of(
                suggestion("Compra habitual", "comprame lo de siempre",
                        SchoolAssistantActionType.COMPRA_ANTICIPADA),
                suggestion("Repetir ultima", "repeti mi ultima compra",
                        SchoolAssistantActionType.COMPRA_ANTICIPADA)
        );
    }

    private SchoolAssistantSuggestion suggestion(
            String label,
            String message,
            SchoolAssistantActionType actionType
    ) {
        return new SchoolAssistantSuggestion(label, message, actionType);
    }

    private SchoolAssistantIntent detectIntent(String message) {
        String normalized = normalize(message);

        return isConfirmation(normalized)
                ? SchoolAssistantIntent.CONFIRM_PURCHASE
                : detectNonConfirmationIntent(normalized);
    }

    private SchoolAssistantIntent detectNonConfirmationIntent(String normalized) {
        return isCancellation(normalized)
                ? SchoolAssistantIntent.CANCEL_ACTION
                : detectRecessOrPurchaseIntent(normalized);
    }

    private SchoolAssistantIntent detectRecessOrPurchaseIntent(String normalized) {
        return recessTime(normalized).isPresent()
                ? SchoolAssistantIntent.SELECT_RECESS
                : detectPurchaseIntent(normalized);
    }

    private SchoolAssistantIntent detectPurchaseIntent(String normalized) {
        return isQuickUsualPurchase(normalized)
                ? SchoolAssistantIntent.BUY_USUAL
                : detectRepeatOrTopicIntent(normalized);
    }

    private SchoolAssistantIntent detectRepeatOrTopicIntent(String normalized) {
        return isRepeatLastPurchase(normalized)
                ? SchoolAssistantIntent.REPEAT_LAST_PURCHASE
                : detectTopicIntent(normalized);
    }

    private SchoolAssistantIntent detectTopicIntent(String normalized) {
        return isFrequentPurchasesQuestion(normalized)
                ? SchoolAssistantIntent.FREQUENT_PURCHASES
                : detectOperationalIntent(normalized);
    }

    private SchoolAssistantIntent detectOperationalIntent(String normalized) {
        if (isWithdrawalCodeQuestion(normalized)) {
            return SchoolAssistantIntent.WITHDRAWAL_CODE;
        }

        if (isPendingOrdersQuestion(normalized)) {
            return SchoolAssistantIntent.PENDING_ORDERS;
        }

        if (isStockQuestion(normalized)) {
            return SchoolAssistantIntent.STOCK;
        }

        if (isBudgetQuestion(normalized)) {
            return SchoolAssistantIntent.BUDGETS;
        }

        if (isRestrictionsQuestion(normalized)) {
            return SchoolAssistantIntent.RESTRICTIONS;
        }

        if (isEventsQuestion(normalized)) {
            return SchoolAssistantIntent.EVENTS;
        }

        if (isChildrenQuestion(normalized)) {
            return SchoolAssistantIntent.CHILDREN;
        }

        if (isMenuQuestion(normalized)) {
            return SchoolAssistantIntent.MENU;
        }

        return detectRecentOrBalanceIntent(normalized);
    }

    private SchoolAssistantIntent detectRecentOrBalanceIntent(String normalized) {
        return isRecentPurchasesQuestion(normalized)
                ? SchoolAssistantIntent.RECENT_PURCHASES
                : detectBalanceIntent(normalized);
    }

    private SchoolAssistantIntent detectBalanceIntent(String normalized) {
        return isBalanceQuestion(normalized)
                ? SchoolAssistantIntent.BALANCE
                : SchoolAssistantIntent.UNKNOWN;
    }

    private boolean isConfirmation(String message) {
        return List.of("confirmar", "confirmo", "si", "dale", "aceptar", "comprar")
                .contains(message);
    }

    private boolean isCancellation(String message) {
        return List.of("cancelar", "cancela", "descartar", "no").contains(message);
    }

    private boolean isQuickUsualPurchase(String message) {
        return containsPurchaseWord(message)
                && containsAny(message, "habitual", "siempre", "lo de siempre");
    }

    private boolean isRepeatLastPurchase(String message) {
        return containsAny(message, "repeti", "repetir", "repetime", "volver a comprar")
                && containsAny(message, "ultima compra", "ultimo pedido");
    }

    private boolean isFrequentPurchasesQuestion(String message) {
        return containsPurchaseWord(message)
                && containsAny(message, "frecuente", "habitual", "suelo", "seguido");
    }

    private boolean isRecentPurchasesQuestion(String message) {
        return containsAny(message, "ultimas compras", "historial de compras", "que compre")
                || containsAny(message, "ultima compra", "compra reciente");
    }

    private boolean isBalanceQuestion(String message) {
        return containsAny(message, "saldo", "credito", "creditos", "disponible");
    }

    private boolean isEventsQuestion(String message) {
        return containsAny(message, "evento", "eventos", "calendario", "acto",
                "jornada", "feriado", "dia escolar");
    }

    private boolean isMenuQuestion(String message) {
        return containsAny(message, "menu", "productos disponibles", "que hay",
                "que venden", "lista de productos");
    }

    private boolean isPendingOrdersQuestion(String message) {
        return containsAny(message, "pedido pendiente", "pedidos pendientes",
                "estado de pedido", "estado de mi pedido", "orden pendiente");
    }

    private boolean isWithdrawalCodeQuestion(String message) {
        return containsAny(message, "codigo de retiro", "codigo retiro",
                "codigo para retirar", "retirar pedido");
    }

    private boolean isStockQuestion(String message) {
        return containsAny(message, "stock", "inventario", "existencias");
    }

    private boolean isChildrenQuestion(String message) {
        return containsAny(message, "mis hijos", "hijos asociados", "hijos");
    }

    private boolean isBudgetQuestion(String message) {
        return containsAny(message, "presupuesto", "presupuestos", "limite");
    }

    private boolean isRestrictionsQuestion(String message) {
        return containsAny(message, "restriccion", "restricciones", "bloqueado",
                "bloqueados", "salud");
    }

    private boolean containsPurchaseWord(String message) {
        return containsAny(message, "compra", "compras", "comprar", "compre", "compro");
    }

    private boolean containsAny(String message, String... values) {
        return List.of(values).stream().anyMatch(message::contains);
    }

    private Optional<RecessTime> recessTime(String message) {
        String normalized = normalize(message);

        if (containsAll(normalized, "primer", "recreo")
                || containsAny(normalized, "first_recess", "primero")) {
            return Optional.of(RecessTime.FIRST_RECESS);
        }

        if (containsAll(normalized, "segundo", "recreo")
                || containsAny(normalized, "second_recess")) {
            return Optional.of(RecessTime.SECOND_RECESS);
        }

        if (containsAny(normalized, "mediodia", "medio dia", "noon", "almuerzo")) {
            return Optional.of(RecessTime.NOON);
        }

        if (containsAny(normalized, "salida", "after_hours", "despues de clase")) {
            return Optional.of(RecessTime.AFTER_HOURS);
        }

        return Optional.empty();
    }

    private boolean containsAll(String message, String... values) {
        return List.of(values).stream().allMatch(message::contains);
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

    private boolean isAssistantMessage(ChatMessage message) {
        return message.role() == RolChatMensaje.ASISTENTE_IA;
    }

    private boolean isTerminalActionStatus(String status) {
        return ACTION_STATUS_EXECUTED.equals(status)
                || ACTION_STATUS_CANCELLED.equals(status)
                || ACTION_STATUS_FAILED.equals(status);
    }

    private boolean isQuickPurchaseAction(Map<String, Object> action) {
        return ACTION_TYPE_QUICK_PURCHASE.equals(value(action.get("tipo")));
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

    private List<Map<String, Object>> purchasesData(List<Purchase> purchases) {
        return purchases.stream()
                .map(this::purchaseData)
                .toList();
    }

    private Map<String, Object> purchaseData(Purchase purchase) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("compraId", value(purchase.getId()));
        data.put("alumnoId", purchase.getStudent() == null
                ? ""
                : value(purchase.getStudent().getId()));
        data.put("buffetId", value(purchase.getBuffetId()));
        data.put("estado", value(purchase.getStatus()));
        data.put("medioPago", value(purchase.getPaymentMethod()));
        data.put("total", purchase.getTotalAmount());
        data.put("fechaHora", value(purchase.getDate()));
        data.put("codigoRetiro", value(purchase.getWithdrawalCode()));
        data.put("fechaRetiro", value(purchase.getPickupDate()));
        data.put("franjaRetiroId", value(purchase.getPickupSlotId()));
        data.put("items", purchaseItemsData(purchase));
        return data;
    }

    private List<Map<String, Object>> purchaseItemsData(Purchase purchase) {
        return purchase.getItems() == null
                ? List.of()
                : purchase.getItems().entrySet()
                        .stream()
                        .filter(entry -> isValidPurchaseItem(entry.getKey(), entry.getValue()))
                        .map(entry -> purchaseItemData(entry.getKey(), entry.getValue()))
                        .toList();
    }

    private Map<String, Object> purchaseItemData(Producto product, Integer quantity) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("productoId", value(product.getId()));
        data.put("nombre", productName(product));
        data.put("cantidad", quantity);
        data.put("precio", price(product));
        return data;
    }

    private List<Map<String, Object>> patternsData(List<PurchasePattern> patterns) {
        return patterns.stream()
                .map(this::patternData)
                .toList();
    }

    private Map<String, Object> patternData(PurchasePattern pattern) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("ocurrencias", pattern.occurrences());
        data.put("ultimaFecha", value(pattern.lastDate()));
        data.put("items", purchaseItemRequestsData(pattern.items()));
        return data;
    }

    private List<Map<String, Object>> purchaseItemRequestsData(
            List<PurchaseItemRequest> items
    ) {
        return items.stream()
                .map(this::purchaseItemRequestData)
                .toList();
    }

    private Map<String, Object> purchaseItemRequestData(PurchaseItemRequest item) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("productoId", item.productId().toString());
        data.put("cantidad", item.quantity());
        data.put("buffetId", value(item.buffetId()));
        return data;
    }

    private Map<String, Object> purchasePlanData(PurchasePlan plan) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("alumnoId", plan.studentId().toString());
        data.put("buffetId", plan.buffetId().toString());
        data.put("totalEstimado", plan.totalAmount());
        data.put("ocurrencias", plan.occurrences());
        data.put("ultimaFecha", value(plan.lastDate()));
        data.put("items", purchasePlanItemsData(plan.items()));
        return data;
    }

    private List<Map<String, Object>> purchasePlanItemsData(
            List<PurchasePlanItem> items
    ) {
        return items.stream()
                .map(this::purchasePlanItemData)
                .toList();
    }

    private Map<String, Object> purchasePlanItemData(PurchasePlanItem item) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("productoId", item.productId().toString());
        data.put("nombre", item.productName());
        data.put("cantidad", item.quantity());
        data.put("precioUnitario", item.unitPrice());
        data.put("subtotal", item.subtotal());
        return data;
    }

    private Map<String, Object> pendingPurchaseData(PendingPurchase pendingPurchase) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("alumnoId", pendingPurchase.studentId().toString());
        data.put("buffetId", pendingPurchase.buffetId().toString());
        data.put("recreo", pendingPurchase.recessTime()
                .map(RecessTime::name)
                .orElse(""));
        data.put("items", purchaseItemRequestsData(pendingPurchase.items()));
        return data;
    }

    private List<Map<String, Object>> productsData(List<Producto> products) {
        return products.stream()
                .map(this::productData)
                .toList();
    }

    private Map<String, Object> productData(Producto product) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("productoId", value(product.getId()));
        data.put("nombre", productName(product));
        data.put("precio", price(product));
        data.put("stock", product.getStockActual());
        data.put("requierePreparacion", product.getRequierePreparacion());
        return data;
    }

    private List<Map<String, Object>> eventsData(List<SchoolCalendarEvent> events) {
        return events.stream()
                .map(this::eventData)
                .toList();
    }

    private Map<String, Object> eventData(SchoolCalendarEvent event) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("fecha", value(event.date()));
        data.put("tipo", value(event.type()));
        data.put("descripcion", value(event.description()));
        return data;
    }

    private List<String> factsByPrefix(SchoolAssistantContext context, String prefix) {
        return context.facts().stream()
                .filter(fact -> fact.startsWith(prefix))
                .map(fact -> fact.substring(prefix.length()))
                .toList();
    }

    private String purchaseList(List<Purchase> purchases) {
        return purchases.stream()
                .map(this::purchaseSummary)
                .collect(Collectors.joining("; "));
    }

    private String productsList(List<Producto> products) {
        return products.stream()
                .map(this::productSummary)
                .collect(Collectors.joining("; "));
    }

    private String productSummary(Producto product) {
        return productName(product)
                + " por "
                + money(price(product))
                + " (stock: "
                + value(product.getStockActual())
                + ")";
    }

    private String eventsList(List<SchoolCalendarEvent> events) {
        return events.stream()
                .map(this::eventSummary)
                .collect(Collectors.joining("; "));
    }

    private String eventSummary(SchoolCalendarEvent event) {
        return value(event.date())
                + " - "
                + value(event.description())
                + " ("
                + value(event.type())
                + ")";
    }

    private String purchaseSummary(Purchase purchase) {
        return date(purchase.getDate())
                + " - "
                + itemListFromPurchase(purchase)
                + " por "
                + money(purchase.getTotalAmount())
                + " ("
                + purchase.getStatus()
                + ")";
    }

    private String patternList(List<PurchasePattern> patterns) {
        return patterns.stream()
                .map(this::patternSummary)
                .collect(Collectors.joining("; "));
    }

    private String patternSummary(PurchasePattern pattern) {
        return itemListFromRequests(pattern.items())
                + " ("
                + pattern.occurrences()
                + " veces)";
    }

    private String itemListFromPurchase(Purchase purchase) {
        return purchase.getItems().entrySet()
                .stream()
                .filter(entry -> isValidPurchaseItem(entry.getKey(), entry.getValue()))
                .map(entry -> entry.getValue() + "x " + productName(entry.getKey()))
                .collect(Collectors.joining(", "));
    }

    private String itemListFromRequests(List<PurchaseItemRequest> items) {
        return items.stream()
                .map(item -> item.quantity() + "x " + productName(item.productId()))
                .collect(Collectors.joining(", "));
    }

    private String itemList(List<PurchasePlanItem> items) {
        return items.stream()
                .map(item -> item.quantity() + "x " + item.productName())
                .collect(Collectors.joining(", "));
    }

    private String productName(UUID productId) {
        return productRepository.findById(productId)
                .map(this::productName)
                .orElse(PRODUCT_WITHOUT_NAME);
    }

    private String productName(Producto product) {
        return product.getNombre() == null ? PRODUCT_WITHOUT_NAME : product.getNombre();
    }

    private BigDecimal price(Producto product) {
        return product.getPrecio() == null ? BigDecimal.ZERO : product.getPrecio();
    }

    private String money(BigDecimal amount) {
        BigDecimal safeAmount = amount == null ? BigDecimal.ZERO : amount;
        return "$" + safeAmount.setScale(2, RoundingMode.HALF_UP);
    }

    private String date(LocalDateTime dateTime) {
        return dateTime == null ? "sin fecha" : DATE_TIME_FORMAT.format(dateTime);
    }

    private String recessLabel(RecessTime recessTime) {
        return switch (recessTime) {
            case FIRST_RECESS -> "primer recreo";
            case SECOND_RECESS -> "segundo recreo";
            case NOON -> "mediodia";
            case AFTER_HOURS -> "salida";
        };
    }

    private String normalize(String message) {
        String lowerCaseMessage = message.toLowerCase(Locale.ROOT).trim();
        String normalizedMessage = Normalizer.normalize(lowerCaseMessage, Normalizer.Form.NFD);
        return DIACRITICS.matcher(normalizedMessage).replaceAll("");
    }

    private Optional<UUID> uuid(Object value) {
        try {
            return Optional.of(UUID.fromString(value(value)));
        } catch (IllegalArgumentException exception) {
            Optional<UUID> empty = Optional.empty();
            return empty;
        }
    }

    private Optional<Integer> integer(Object value) {
        return value instanceof Number number
                ? Optional.of(number.intValue())
                : integerFromString(value);
    }

    private Optional<Integer> integerFromString(Object value) {
        try {
            return Optional.of(Integer.parseInt(value(value)));
        } catch (NumberFormatException exception) {
            Optional<Integer> empty = Optional.empty();
            return empty;
        }
    }

    private String value(Object value) {
        return value == null ? "" : value.toString();
    }

    private Map<String, Object> castMap(Map<?, ?> source) {
        Map<String, Object> result = new LinkedHashMap<>();
        source.forEach((key, value) -> result.put(key.toString(), value));
        return result;
    }

    private record PurchaseItemRequest(
            UUID productId,
            int quantity,
            UUID buffetId
    ) {
    }

    private record PurchasePlanItem(
            UUID productId,
            String productName,
            int quantity,
            BigDecimal unitPrice
    ) {
        private BigDecimal subtotal() {
            return unitPrice.multiply(BigDecimal.valueOf(quantity));
        }
    }

    private record PurchasePlan(
            UUID studentId,
            UUID buffetId,
            List<PurchasePlanItem> items,
            int occurrences,
            LocalDateTime lastDate
    ) {
        private BigDecimal totalAmount() {
            return items.stream()
                    .map(PurchasePlanItem::subtotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
    }

    private record PurchasePatternCandidate(
            String key,
            List<PurchaseItemRequest> items,
            LocalDateTime lastDate
    ) {
    }

    private record PurchasePattern(
            List<PurchaseItemRequest> items,
            int occurrences,
            LocalDateTime lastDate
    ) {
    }

    private record PendingPurchase(
            UUID studentId,
            UUID buffetId,
            List<PurchaseItemRequest> items,
            Optional<RecessTime> recessTime
    ) {
    }

    private record PendingActionSearch(
            Optional<PendingPurchase> pendingPurchase,
            boolean shouldStop
    ) {
        private static PendingActionSearch terminal() {
            return new PendingActionSearch(Optional.empty(), true);
        }

        private static PendingActionSearch from(Optional<PendingPurchase> pendingPurchase) {
            return new PendingActionSearch(pendingPurchase, pendingPurchase.isPresent());
        }
    }

    private static final class PurchasePatternAccumulator {

        private final List<PurchaseItemRequest> items;
        private final int occurrences;
        private final LocalDateTime lastDate;

        private PurchasePatternAccumulator(PurchasePatternCandidate candidate) {
            this(candidate.items(), 1, candidate.lastDate());
        }

        private PurchasePatternAccumulator(
                List<PurchaseItemRequest> items,
                int occurrences,
                LocalDateTime lastDate
        ) {
            this.items = items;
            this.occurrences = occurrences;
            this.lastDate = lastDate;
        }

        private PurchasePatternAccumulator merge(PurchasePatternAccumulator other) {
            return new PurchasePatternAccumulator(
                    this.items,
                    this.occurrences + other.occurrences,
                    latestDate(this.lastDate, other.lastDate)
            );
        }

        private PurchasePattern pattern() {
            return new PurchasePattern(items, occurrences, lastDate);
        }

        private LocalDateTime latestDate(LocalDateTime first, LocalDateTime second) {
            return Comparator.nullsLast(LocalDateTime::compareTo)
                    .compare(first, second) >= 0
                    ? first
                    : second;
        }
    }
}
