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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

class SchoolAssistantFixedTopicResponses {

    private static final int RECENT_PURCHASES_TO_SHOW = 3;
    private static final int FREQUENT_PURCHASES_TO_SHOW = 3;

    private final PurchaseRepository purchaseRepository;
    private final ProductRepository productRepository;
    private final SchoolAssistantPurchasePatternService purchasePatternService;
    private final SchoolAssistantReplyFormatter replyFormatter;
    private final SchoolAssistantContentFormatter contentFormatter;
    private final SchoolAssistantTutorFactsFormatter tutorFactsFormatter;
    private final SchoolAssistantReplyBuilder replyBuilder;

    SchoolAssistantFixedTopicResponses(
            PurchaseRepository purchaseRepository,
            ProductRepository productRepository,
            SchoolAssistantPurchasePatternService purchasePatternService,
            SchoolAssistantReplyFormatter replyFormatter,
            SchoolAssistantContentFormatter contentFormatter,
            SchoolAssistantTutorFactsFormatter tutorFactsFormatter,
            SchoolAssistantReplyBuilder replyBuilder
    ) {
        this.purchaseRepository = purchaseRepository;
        this.productRepository = productRepository;
        this.purchasePatternService = purchasePatternService;
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
        List<PurchasePattern> patterns = purchasePatternService.frequentPatterns(
                context.studentId(),
                FREQUENT_PURCHASES_TO_SHOW
        );

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
        return product.hasAvailableStock();
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

    private List<EstadoCompra> pendingStatuses() {
        return List.of(
                EstadoCompra.PENDIENTE,
                EstadoCompra.EN_PREPARACION,
                EstadoCompra.LISTO
        );
    }

    private List<Purchase> pendingStudentPurchases(UUID studentId) {
        return purchaseHistory(studentId).stream()
                .filter(Objects::nonNull)
                .filter(Purchase::isActivePendingOrder)
                .limit(RECENT_PURCHASES_TO_SHOW)
                .toList();
    }

    private boolean hasWithdrawalCode(Purchase purchase) {
        return purchase.hasWithdrawalCode();
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
