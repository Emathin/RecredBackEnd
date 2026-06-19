package com.recred.backend.application;

import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.SchoolAssistantAction;
import com.recred.backend.domain.models.SchoolAssistantActionType;
import com.recred.backend.domain.models.SchoolAssistantContext;
import com.recred.backend.domain.models.SchoolAssistantIntent;
import com.recred.backend.domain.models.SchoolCalendarEvent;
import com.recred.backend.domain.repositories.ProductRepository;
import com.recred.backend.domain.repositories.PurchaseRepository;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

class SchoolAssistantFixedTopicResponses {

    private static final int RECENT_PURCHASES_TO_SHOW = 3;
    private static final int FREQUENT_PURCHASES_TO_SHOW = 3;
    private static final int PURCHASE_PATTERN_LOOKBACK = 30;
    private static final int MIN_FREQUENT_PURCHASE_OCCURRENCES = 2;

    private final PurchaseRepository purchaseRepository;
    private final ProductRepository productRepository;
    private final SchoolAssistantReplyFormatter replyFormatter;
    private final SchoolAssistantContentFormatter contentFormatter;
    private final SchoolAssistantTutorFactsFormatter tutorFactsFormatter;
    private final SchoolAssistantReplyBuilder replyBuilder;

    SchoolAssistantFixedTopicResponses(
            PurchaseRepository purchaseRepository,
            ProductRepository productRepository,
            SchoolAssistantReplyFormatter replyFormatter,
            SchoolAssistantContentFormatter contentFormatter,
            SchoolAssistantTutorFactsFormatter tutorFactsFormatter,
            SchoolAssistantReplyBuilder replyBuilder
    ) {
        this.purchaseRepository = purchaseRepository;
        this.productRepository = productRepository;
        this.replyFormatter = replyFormatter;
        this.contentFormatter = contentFormatter;
        this.tutorFactsFormatter = tutorFactsFormatter;
        this.replyBuilder = replyBuilder;
    }

    SchoolAssistantFixedReply forbiddenQuickPurchaseReply(
            SchoolAssistantFixedTopicRequest request
    ) {
        return reply(
                "Por ahora solo los alumnos pueden hacer compras rapidas "
                        + "desde el asistente.",
                request.intent(),
                forbiddenQuickPurchaseAction(),
                request.context().role()
        );
    }

    SchoolAssistantFixedReply kiosqueroPendingOrdersReply(
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
                ? reply("No hay pedidos pendientes en el buffet por ahora.",
                        SchoolAssistantIntent.PENDING_ORDERS, action, context.role())
                : reply("Pedidos pendientes:\n" + contentFormatter.purchaseList(purchases),
                        SchoolAssistantIntent.PENDING_ORDERS, action, context.role());
    }

    SchoolAssistantFixedReply stockReply(SchoolAssistantContext context) {
        List<Producto> products = productRepository.findAllActiveWithStock(context.buffetId());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("productos", contentFormatter.productsData(products));
        SchoolAssistantAction action = informativeAction(
                SchoolAssistantActionType.CONSULTA_STOCK,
                data
        );

        return products.isEmpty()
                ? reply("No veo productos activos con stock por ahora.",
                        SchoolAssistantIntent.STOCK, action, context.role())
                : reply("Stock actual (" + replyFormatter.countText(
                        products.size(),
                        "producto",
                        "productos"
                )
                        + "):\n" + contentFormatter.productsList(products),
                        SchoolAssistantIntent.STOCK, action, context.role());
    }

    SchoolAssistantFixedReply childrenReply(SchoolAssistantContext context) {
        List<String> children = factsByPrefix(context, "hijo=");
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("hijos", children);
        SchoolAssistantAction action = informativeAction(
                SchoolAssistantActionType.CONSULTA_HIJOS,
                data
        );

        List<String> visibleChildren = tutorFactsFormatter.children(children);

        return children.isEmpty()
                ? reply("No veo hijos asociados a tu cuenta.",
                        SchoolAssistantIntent.CHILDREN, action, context.role())
                : reply("Hijos asociados:\n" + replyFormatter.formattedItems(visibleChildren),
                        SchoolAssistantIntent.CHILDREN, action, context.role());
    }

    SchoolAssistantFixedReply tutorFactsReply(
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
                ? reply("No veo datos de hijos asociados a tu cuenta.", intent, action,
                        context.role())
                : reply(
                        prefix + ":\n" + replyFormatter.formattedItems(visibleChildren),
                        intent,
                        action,
                        context.role()
                );
    }

    SchoolAssistantFixedReply balanceReply(SchoolAssistantContext context) {
        String answer = context.balance()
                .map(balance -> "Tenes " + contentFormatter.money(balance) + " disponibles.")
                .orElse("Todavia no veo saldo registrado en tu cuenta.");
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("saldo", context.balance().orElse(null));
        return reply(
                answer,
                SchoolAssistantIntent.BALANCE,
                informativeAction(SchoolAssistantActionType.CONSULTA_SALDO, data)
        );
    }

    SchoolAssistantFixedReply recentPurchasesReply(SchoolAssistantContext context) {
        List<Purchase> purchases = purchaseHistory(context.studentId()).stream()
                .limit(RECENT_PURCHASES_TO_SHOW)
                .toList();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("compras", contentFormatter.purchasesData(purchases));

        return purchases.isEmpty()
                ? reply(
                        "Todavia no veo compras registradas en tu historial.",
                        SchoolAssistantIntent.RECENT_PURCHASES,
                        informativeAction(SchoolAssistantActionType.CONSULTA_COMPRAS, data)
                )
                : reply(
                        "Tus ultimas compras:\n" + contentFormatter.purchaseList(purchases),
                        SchoolAssistantIntent.RECENT_PURCHASES,
                        informativeAction(SchoolAssistantActionType.CONSULTA_COMPRAS, data)
                );
    }

    SchoolAssistantFixedReply frequentPurchasesReply(SchoolAssistantContext context) {
        List<PurchasePattern> patterns = purchasePatterns(context.studentId()).stream()
                .limit(FREQUENT_PURCHASES_TO_SHOW)
                .toList();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("patrones", contentFormatter.patternsData(patterns));

        return patterns.isEmpty()
                ? reply(
                        "Todavia no tengo suficientes compras tuyas para detectar "
                                + "compras frecuentes.",
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

    SchoolAssistantFixedReply eventsReply(SchoolAssistantContext context) {
        List<SchoolCalendarEvent> events = context.upcomingEvents();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("eventos", contentFormatter.eventsData(events));
        SchoolAssistantAction action = informativeAction(
                SchoolAssistantActionType.CONSULTA_EVENTOS,
                data
        );

        return events.isEmpty()
                ? reply("No veo eventos escolares proximos por ahora.",
                        SchoolAssistantIntent.EVENTS,
                        action, context.role())
                : reply("Eventos proximos:\n" + contentFormatter.eventsList(events),
                        SchoolAssistantIntent.EVENTS, action, context.role());
    }

    SchoolAssistantFixedReply menuReply(SchoolAssistantContext context) {
        Optional<UUID> buffetId = lastBuffetId(context.studentId());

        if (buffetId.isEmpty()) {
            return reply(
                    "Todavia no se que buffet usar para mostrarte productos.",
                    SchoolAssistantIntent.MENU,
                    informativeAction(SchoolAssistantActionType.CONSULTA_MENU, Map.of())
            );
        }

        List<Producto> products = productRepository.findAllActiveWithStock(buffetId.orElseThrow())
                .stream()
                .filter(this::hasAvailability)
                .toList();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("buffetId", buffetId.orElseThrow().toString());
        data.put("productos", contentFormatter.productsData(products));
        SchoolAssistantAction action = informativeAction(
                SchoolAssistantActionType.CONSULTA_MENU,
                data
        );

        return products.isEmpty()
                ? reply("No veo productos disponibles para ese buffet por ahora.",
                        SchoolAssistantIntent.MENU, action)
                : reply("Productos disponibles ("
                        + replyFormatter.countText(products.size(), "producto", "productos")
                        + "):\n" + contentFormatter.productsList(products),
                        SchoolAssistantIntent.MENU, action);
    }

    SchoolAssistantFixedReply studentPendingOrdersReply(
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

    SchoolAssistantFixedReply studentWithdrawalCodeReply(
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
                        "No veo ningun pedido pendiente con codigo de retiro.",
                        SchoolAssistantIntent.WITHDRAWAL_CODE,
                        action
                ));
    }

    private boolean hasAvailability(Producto product) {
        return product.getStockDisponible() != null && product.getStockDisponible() > 0;
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
            com.recred.backend.domain.models.SchoolAssistantRole role
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
