package com.recred.backend.application;

import com.recred.backend.domain.exceptions.InsufficientBalanceException;
import com.recred.backend.domain.exceptions.PresupuestoExcedidoException;
import com.recred.backend.domain.exceptions.RestriccionHorariaException;
import com.recred.backend.domain.exceptions.RestriccionSaludException;
import com.recred.backend.domain.models.ChatMessage;
import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.RolChatMensaje;
import com.recred.backend.domain.models.SchoolAssistantContext;
import com.recred.backend.domain.models.SchoolAssistantRole;
import com.recred.backend.domain.repositories.ProductRepository;
import com.recred.backend.domain.repositories.PurchaseRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
    private final ProcessPresentialPurchase processPresentialPurchase;

    public SchoolAssistantFixedTopics(
            PurchaseRepository purchaseRepository,
            ProductRepository productRepository,
            ProcessPresentialPurchase processPresentialPurchase
    ) {
        this.purchaseRepository = purchaseRepository;
        this.productRepository = productRepository;
        this.processPresentialPurchase = processPresentialPurchase;
    }

    public Optional<SchoolAssistantFixedReply> respond(
            String message,
            SchoolAssistantContext context,
            List<ChatMessage> recentMessages
    ) {
        SchoolAssistantIntent intent = detectIntent(message);

        if (context.role() != SchoolAssistantRole.ALUMNO) {
            Optional<SchoolAssistantFixedReply> reply = nonStudentReply(intent);
            return reply;
        }

        return studentReply(intent, context, recentMessages);
    }

    private Optional<SchoolAssistantFixedReply> studentReply(
            SchoolAssistantIntent intent,
            SchoolAssistantContext context,
            List<ChatMessage> recentMessages
    ) {
        return switch (intent) {
            case BALANCE -> Optional.of(balanceReply(context));
            case RECENT_PURCHASES -> Optional.of(recentPurchasesReply(context));
            case FREQUENT_PURCHASES -> Optional.of(frequentPurchasesReply(context));
            case REPEAT_LAST_PURCHASE -> Optional.of(repeatLastPurchaseReply(context));
            case BUY_USUAL -> Optional.of(buyUsualPurchaseReply(context));
            case CONFIRM_PURCHASE -> Optional.of(confirmPurchaseReply(context, recentMessages));
            case CANCEL_ACTION -> Optional.of(cancelActionReply(recentMessages));
            case UNKNOWN -> Optional.empty();
        };
    }

    private Optional<SchoolAssistantFixedReply> nonStudentReply(SchoolAssistantIntent intent) {
        return switch (intent) {
            case REPEAT_LAST_PURCHASE, BUY_USUAL, CONFIRM_PURCHASE, CANCEL_ACTION ->
                    Optional.of(reply(
                            "Por ahora las compras rapidas desde el asistente "
                                    + "estan disponibles para alumnos.",
                            intent.name()
                    ));
            default -> Optional.empty();
        };
    }

    private SchoolAssistantFixedReply balanceReply(SchoolAssistantContext context) {
        String answer = context.balance()
                .map(balance -> "Tenes " + money(balance) + " disponibles.")
                .orElse("No encontre saldo registrado para este alumno.");
        return reply(answer, SchoolAssistantIntent.BALANCE.name());
    }

    private SchoolAssistantFixedReply recentPurchasesReply(SchoolAssistantContext context) {
        List<Purchase> purchases = purchaseHistory(context.studentId()).stream()
                .limit(RECENT_PURCHASES_TO_SHOW)
                .toList();

        return purchases.isEmpty()
                ? reply("No encontre compras registradas para este alumno.",
                        SchoolAssistantIntent.RECENT_PURCHASES.name())
                : reply("Tus ultimas compras son: " + purchaseList(purchases) + ".",
                        SchoolAssistantIntent.RECENT_PURCHASES.name());
    }

    private SchoolAssistantFixedReply frequentPurchasesReply(SchoolAssistantContext context) {
        List<PurchasePattern> patterns = purchasePatterns(context.studentId()).stream()
                .limit(FREQUENT_PURCHASES_TO_SHOW)
                .toList();

        return patterns.isEmpty()
                ? reply("Todavia no detecte compras frecuentes para este alumno.",
                        SchoolAssistantIntent.FREQUENT_PURCHASES.name())
                : reply("Tus compras frecuentes son: " + patternList(patterns) + ".",
                        SchoolAssistantIntent.FREQUENT_PURCHASES.name());
    }

    private SchoolAssistantFixedReply repeatLastPurchaseReply(SchoolAssistantContext context) {
        Optional<Purchase> purchase = purchaseHistory(context.studentId()).stream()
                .filter(this::isRebuyablePurchase)
                .findFirst();

        return purchase
                .flatMap(lastPurchase -> planFromPurchase(context.studentId(), lastPurchase, 1))
                .map(plan -> pendingPurchaseReply(
                        plan,
                        SchoolAssistantIntent.REPEAT_LAST_PURCHASE.name(),
                        "Vas a repetir tu ultima compra"
                ))
                .orElseGet(() -> reply(
                        "No encontre una compra entregada disponible para repetir.",
                        SchoolAssistantIntent.REPEAT_LAST_PURCHASE.name()
                ));
    }

    private SchoolAssistantFixedReply buyUsualPurchaseReply(SchoolAssistantContext context) {
        return mostFrequentPlan(context.studentId())
                .map(plan -> pendingPurchaseReply(
                        plan,
                        SchoolAssistantIntent.BUY_USUAL.name(),
                        "Tu compra habitual detectada"
                ))
                .orElseGet(() -> reply(
                        "Todavia no detecte una compra habitual disponible para repetir.",
                        SchoolAssistantIntent.BUY_USUAL.name()
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
                        SchoolAssistantIntent.CONFIRM_PURCHASE.name()
                ));
    }

    private SchoolAssistantFixedReply cancelActionReply(List<ChatMessage> recentMessages) {
        boolean hasPendingPurchase = pendingPurchase(recentMessages).isPresent();
        String answer = hasPendingPurchase
                ? "Listo, descarte la compra pendiente."
                : "No hay una compra pendiente para cancelar.";
        Map<String, Object> metadata = metadata(SchoolAssistantIntent.CANCEL_ACTION.name());
        metadata.put(ACTION_STATUS_KEY, ACTION_STATUS_CANCELLED);
        return new SchoolAssistantFixedReply(answer, metadata);
    }

    private SchoolAssistantFixedReply executePendingPurchase(
            SchoolAssistantContext context,
            PendingPurchase pendingPurchase
    ) {
        UUID studentId = context.studentId();
        Optional<Map<Producto, Integer>> cart = cart(pendingPurchase);

        if (studentDoesNotOwnPendingPurchase(studentId, pendingPurchase)) {
            SchoolAssistantFixedReply reply = failedPurchaseReply(
                    "La compra pendiente no corresponde a este alumno."
            );
            return reply;
        }

        if (cart.isEmpty()) {
            SchoolAssistantFixedReply reply = failedPurchaseReply(
                    "No pude realizar la compra porque algun producto ya no esta disponible."
            );
            return reply;
        }

        try {
            Purchase purchase = processPresentialPurchase.execute(studentId, cart.orElseThrow());
            return executedPurchaseReply(purchase);
        } catch (InsufficientBalanceException
                 | PresupuestoExcedidoException
                 | RestriccionHorariaException
                 | RestriccionSaludException exception) {
            return failedPurchaseReply("No pude realizar la compra: " + exception.getMessage());
        }
    }

    private SchoolAssistantFixedReply executedPurchaseReply(Purchase purchase) {
        Map<String, Object> metadata = metadata(
                SchoolAssistantIntent.CONFIRM_PURCHASE.name()
        );
        metadata.put(ACTION_STATUS_KEY, ACTION_STATUS_EXECUTED);
        metadata.put("compraId", purchase.getId().toString());

        return new SchoolAssistantFixedReply(
                "Compra realizada. Total: " + money(purchase.getTotalAmount())
                        + ". Estado: " + purchase.getStatus() + ".",
                metadata
        );
    }

    private SchoolAssistantFixedReply failedPurchaseReply(String answer) {
        Map<String, Object> metadata = metadata(
                SchoolAssistantIntent.CONFIRM_PURCHASE.name()
        );
        metadata.put(ACTION_STATUS_KEY, ACTION_STATUS_FAILED);
        return new SchoolAssistantFixedReply(answer, metadata);
    }

    private SchoolAssistantFixedReply pendingPurchaseReply(
            PurchasePlan plan,
            String intent,
            String prefix
    ) {
        Map<String, Object> metadata = metadata(intent);
        metadata.put(ACTION_STATUS_KEY, ACTION_STATUS_PENDING);
        metadata.put(PENDING_ACTION_KEY, pendingAction(plan));

        return new SchoolAssistantFixedReply(
                prefix + ": " + itemList(plan.items())
                        + ". Total estimado: " + money(plan.totalAmount())
                        + ". Escribi confirmar para comprar o cancelar para descartarla.",
                metadata
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

        return items.size() == requests.size() && !items.isEmpty()
                ? Optional.of(new PurchasePlan(studentId, items, occurrences, lastDate))
                : Optional.empty();
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
                        entry.getValue()
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
        List<PurchaseItemRequest> items = itemsFromAction(action.get("items"));

        return studentId.isPresent() && !items.isEmpty()
                ? Optional.of(new PendingPurchase(studentId.orElseThrow(), items))
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
                        quantity.orElseThrow()
                ))
                : Optional.empty();
    }

    private Map<String, Object> pendingAction(PurchasePlan plan) {
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("tipo", ACTION_TYPE_QUICK_PURCHASE);
        action.put("alumnoId", plan.studentId().toString());
        action.put("items", plan.items().stream()
                .map(this::pendingActionItem)
                .toList());
        return action;
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

    private SchoolAssistantFixedReply reply(String answer, String intent) {
        return new SchoolAssistantFixedReply(answer, metadata(intent));
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
                : detectRecentOrBalanceIntent(normalized);
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

    private boolean containsPurchaseWord(String message) {
        return containsAny(message, "compra", "compras", "comprar", "compre", "compro");
    }

    private boolean containsAny(String message, String... values) {
        return List.of(values).stream().anyMatch(message::contains);
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

    private String purchaseList(List<Purchase> purchases) {
        return purchases.stream()
                .map(this::purchaseSummary)
                .collect(Collectors.joining("; "));
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

    private enum SchoolAssistantIntent {
        BALANCE,
        RECENT_PURCHASES,
        FREQUENT_PURCHASES,
        REPEAT_LAST_PURCHASE,
        BUY_USUAL,
        CONFIRM_PURCHASE,
        CANCEL_ACTION,
        UNKNOWN
    }

    private record PurchaseItemRequest(
            UUID productId,
            int quantity
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
            List<PurchaseItemRequest> items
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
