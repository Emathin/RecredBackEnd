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

class SchoolAssistantQuickPurchaseHandler {

    private static final int PURCHASE_PATTERN_LOOKBACK = 30;
    private static final int MIN_FREQUENT_PURCHASE_OCCURRENCES = 2;
    private static final String ACTION_STATUS_KEY = "estadoAccionChatbot";
    private static final String PENDING_ACTION_KEY = "accionPendienteChatbot";
    private static final String ACTION_TYPE_QUICK_PURCHASE = "COMPRA_RAPIDA";
    private static final List<String> RECESS_OPTIONS = List.of(
            "primer recreo",
            "segundo recreo",
            "mediodia",
            "salida"
    );
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
    private final SchoolAssistantPurchaseFailureMessages purchaseFailureMessages;
    private final SchoolAssistantSuggestionFactory suggestionFactory;
    private final SchoolAssistantPendingPurchaseParser pendingPurchaseParser;
    private final SchoolAssistantReplyBuilder replyBuilder;

    SchoolAssistantQuickPurchaseHandler(
            PurchaseRepository purchaseRepository,
            ProductRepository productRepository,
            CreateAssistantAdvancePurchase createAssistantAdvancePurchase,
            Clock clock,
            SchoolAssistantReplyFormatter replyFormatter,
            SchoolAssistantContentFormatter contentFormatter,
            SchoolAssistantPurchaseFailureMessages purchaseFailureMessages,
            SchoolAssistantSuggestionFactory suggestionFactory,
            SchoolAssistantPendingPurchaseParser pendingPurchaseParser,
            SchoolAssistantReplyBuilder replyBuilder
    ) {
        this.purchaseRepository = purchaseRepository;
        this.productRepository = productRepository;
        this.createAssistantAdvancePurchase = createAssistantAdvancePurchase;
        this.clock = clock;
        this.replyFormatter = replyFormatter;
        this.contentFormatter = contentFormatter;
        this.purchaseFailureMessages = purchaseFailureMessages;
        this.suggestionFactory = suggestionFactory;
        this.pendingPurchaseParser = pendingPurchaseParser;
        this.replyBuilder = replyBuilder;
    }

    SchoolAssistantFixedReply repeatLastPurchaseReply(SchoolAssistantContext context) {
        Optional<Purchase> purchase = purchaseHistory(context.studentId()).stream()
                .filter(this::isRebuyablePurchase)
                .findFirst();

        return purchase
                .flatMap(lastPurchase -> planFromPurchase(context.studentId(), lastPurchase, 1))
                .map(plan -> pendingPurchaseReply(
                        plan,
                        SchoolAssistantIntent.REPEAT_LAST_PURCHASE,
                        "Puedo repetir tu ultima compra con estos productos"
                ))
                .orElseGet(() -> reply(
                        "Todavia no encontre una compra tuya que pueda repetir.",
                        SchoolAssistantIntent.REPEAT_LAST_PURCHASE,
                        failedAdvanceAction(Map.of())
                ));
    }

    SchoolAssistantFixedReply buyUsualPurchaseReply(SchoolAssistantContext context) {
        return mostFrequentPlan(context.studentId())
                .map(plan -> pendingPurchaseReply(
                        plan,
                        SchoolAssistantIntent.BUY_USUAL,
                        "Detecte esta compra como habitual"
                ))
                .orElseGet(() -> reply(
                        "Todavia no tengo suficientes compras tuyas para armar "
                                + "una compra habitual.",
                        SchoolAssistantIntent.BUY_USUAL,
                        failedAdvanceAction(Map.of())
                ));
    }

    SchoolAssistantFixedReply confirmPurchaseReply(
            SchoolAssistantContext context,
            List<ChatMessage> recentMessages
    ) {
        return pendingPurchaseParser.pendingPurchase(recentMessages)
                .map(pendingPurchase -> executePendingPurchase(context, pendingPurchase))
                .orElseGet(() -> reply(
                        "No tengo ninguna compra pendiente para confirmar.",
                        SchoolAssistantIntent.CONFIRM_PURCHASE,
                        failedAdvanceAction(Map.of())
                ));
    }

    SchoolAssistantFixedReply selectRecessReply(
            SchoolAssistantContext context,
            List<ChatMessage> recentMessages,
            RecessTime recessTime
    ) {
        return pendingPurchaseParser.pendingPurchase(recentMessages)
                .map(pendingPurchase -> withSelectedRecessReply(context, pendingPurchase,
                        recessTime))
                .orElseGet(() -> reply(
                        "No tengo ninguna compra pendiente para elegir recreo.",
                        SchoolAssistantIntent.SELECT_RECESS,
                        failedAdvanceAction(Map.of())
                ));
    }

    SchoolAssistantFixedReply cancelActionReply(List<ChatMessage> recentMessages) {
        boolean hasPendingPurchase = pendingPurchaseParser.pendingPurchase(recentMessages)
                .isPresent();
        String answer = hasPendingPurchase
                ? "Listo, descarte esa compra pendiente."
                : "No tengo ninguna compra pendiente para cancelar.";
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
                    "Esa compra pendiente no corresponde a tu usuario."
            );
            return reply;
        }

        if (pendingPurchase.recessTime().isEmpty()) {
            return missingRecessReply(pendingPurchase);
        }

        Optional<Map<Producto, Integer>> cart = cart(pendingPurchase);

        if (cart.isEmpty()) {
            SchoolAssistantFixedReply reply = failedPurchaseReply(
                    "No pude realizar la compra porque algun producto ya no esta "
                            + "disponible."
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
            return failedPurchaseReply("Esa compra pendiente no corresponde a tu usuario.");
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
                        + ".\nEscribi confirmar para crear la compra anticipada o cancelar.",
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
                "Listo, tu compra anticipada quedo creada:\n"
                        + "- Total: "
                        + contentFormatter.money(purchase.getTotalAmount())
                        + "\n- Estado: "
                        + contentFormatter.purchaseStatusLabel(purchase.getStatus())
                        + "\n- Codigo de retiro: "
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
                recessSelectionText(),
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
                prefix + ":\n" + contentFormatter.purchasePlanItemList(plan.items())
                        + "\n\nTotal estimado: "
                        + contentFormatter.money(plan.totalAmount())
                        + "\n" + recessSelectionText(),
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

    private String recessSelectionText() {
        return "Para terminar, elegi el recreo de retiro:\n"
                + replyFormatter.formattedItems(RECESS_OPTIONS);
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
}
