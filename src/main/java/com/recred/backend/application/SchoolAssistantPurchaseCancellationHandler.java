package com.recred.backend.application;

import com.recred.backend.domain.exceptions.InvalidPurchaseStatusTransitionException;
import com.recred.backend.domain.exceptions.PurchaseAccessDeniedException;
import com.recred.backend.domain.models.ChatMessage;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.RolChatMensaje;
import com.recred.backend.domain.models.SchoolAssistantAction;
import com.recred.backend.domain.models.SchoolAssistantActionStatus;
import com.recred.backend.domain.models.SchoolAssistantActionType;
import com.recred.backend.domain.models.SchoolAssistantContext;
import com.recred.backend.domain.models.SchoolAssistantIntent;
import com.recred.backend.domain.repositories.PurchaseRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

class SchoolAssistantPurchaseCancellationHandler {

    private static final int CANCELABLE_PURCHASES_TO_SHOW = 3;
    private static final String ACTION_STATUS_KEY = "estadoAccionChatbot";
    private static final String ACTION_KEY = "accion";
    private static final String PURCHASE_ID_KEY = "compraId";
    private static final String ACTION_STATUS_EXECUTED = "EJECUTADA";

    private final PurchaseRepository purchaseRepository;
    private final CancelPresentialPurchase cancelPresentialPurchase;
    private final SchoolAssistantContentFormatter contentFormatter;
    private final SchoolAssistantReplyBuilder replyBuilder;

    SchoolAssistantPurchaseCancellationHandler(
            PurchaseRepository purchaseRepository,
            CancelPresentialPurchase cancelPresentialPurchase,
            SchoolAssistantContentFormatter contentFormatter,
            SchoolAssistantReplyBuilder replyBuilder
    ) {
        this.purchaseRepository = purchaseRepository;
        this.cancelPresentialPurchase = cancelPresentialPurchase;
        this.contentFormatter = contentFormatter;
        this.replyBuilder = replyBuilder;
    }

    SchoolAssistantFixedReply cancelPurchaseReply(
            SchoolAssistantContext context,
            List<ChatMessage> recentMessages
    ) {
        Optional<UUID> recentPurchaseId = recentCreatedPurchaseId(recentMessages);

        if (recentPurchaseId.isPresent()) {
            return cancelPurchase(context, recentPurchaseId.orElseThrow());
        }

        List<Purchase> cancelablePurchases = cancelablePurchases(context.studentId());

        if (cancelablePurchases.isEmpty()) {
            return failedReply("No encontre compras pendientes que puedas cancelar.", Map.of());
        }

        if (cancelablePurchases.size() > 1) {
            return multipleCancelablePurchasesReply(cancelablePurchases);
        }

        return cancelPurchase(context, cancelablePurchases.get(0).getId());
    }

    private SchoolAssistantFixedReply cancelPurchase(
            SchoolAssistantContext context,
            UUID purchaseId
    ) {
        try {
            Purchase purchase = cancelPresentialPurchase.execute(
                    new CancelPresentialPurchaseRequest(purchaseId, context.studentId()));
            return cancelledReply(purchase);
        } catch (InvalidPurchaseStatusTransitionException
                 | PurchaseAccessDeniedException
                 | IllegalArgumentException exception) {
            return failedReply(failureMessage(exception), Map.of(
                    PURCHASE_ID_KEY,
                    purchaseId.toString()
            ));
        }
    }

    private SchoolAssistantFixedReply cancelledReply(Purchase purchase) {
        Map<String, Object> data = contentFormatter.purchaseData(purchase);
        SchoolAssistantAction action = new SchoolAssistantAction(
                SchoolAssistantActionType.CANCELACION_COMPRA,
                SchoolAssistantActionStatus.EJECUTADA,
                false,
                List.of(),
                purchase.getId(),
                purchase.getStatus(),
                purchase.getWithdrawalCode(),
                purchase.getTotalAmount(),
                null,
                data
        );

        return replyBuilder.reply(
                "Listo, cancele tu compra y te devolvi "
                        + contentFormatter.money(purchase.getTotalAmount())
                        + " en creditos.",
                SchoolAssistantIntent.CANCEL_PURCHASE,
                action
        );
    }

    private SchoolAssistantFixedReply multipleCancelablePurchasesReply(
            List<Purchase> purchases
    ) {
        List<Purchase> visiblePurchases = purchases.stream()
                .limit(CANCELABLE_PURCHASES_TO_SHOW)
                .toList();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("comprasCancelables", contentFormatter.purchasesData(purchases));
        SchoolAssistantAction action = SchoolAssistantAction.informative(
                SchoolAssistantActionType.CANCELACION_COMPRA,
                data
        );

        return replyBuilder.reply(
                "Tenes mas de una compra pendiente. Para evitar cancelar una compra "
                        + "equivocada, revisa tus pedidos pendientes:\n"
                        + contentFormatter.purchaseList(visiblePurchases),
                SchoolAssistantIntent.CANCEL_PURCHASE,
                action
        );
    }

    private SchoolAssistantFixedReply failedReply(String answer, Map<String, Object> data) {
        SchoolAssistantAction action = new SchoolAssistantAction(
                SchoolAssistantActionType.CANCELACION_COMPRA,
                SchoolAssistantActionStatus.FALLIDA,
                false,
                List.of(),
                null,
                null,
                null,
                null,
                null,
                data
        );
        return replyBuilder.reply(answer, SchoolAssistantIntent.CANCEL_PURCHASE, action);
    }

    private List<Purchase> cancelablePurchases(UUID studentId) {
        return purchaseRepository.findHistorialByAlumno(studentId)
                .stream()
                .filter(this::isCancelablePurchase)
                .toList();
    }

    private boolean isCancelablePurchase(Purchase purchase) {
        return purchase != null
                && purchase.getId() != null
                && purchase.isPending();
    }

    private Optional<UUID> recentCreatedPurchaseId(List<ChatMessage> recentMessages) {
        return recentMessages.stream()
                .filter(this::isAssistantMessage)
                .map(ChatMessage::metadata)
                .filter(metadata -> metadata != null && !metadata.isEmpty())
                .map(this::createdPurchaseId)
                .flatMap(Optional::stream)
                .findFirst();
    }

    private Optional<UUID> createdPurchaseId(Map<String, Object> metadata) {
        Optional<UUID> topLevelPurchaseId = topLevelCreatedPurchaseId(metadata);

        if (topLevelPurchaseId.isPresent()) {
            return topLevelPurchaseId;
        }

        return action(metadata)
                .filter(this::isExecutedAdvancePurchaseAction)
                .flatMap(action -> uuid(action.get(PURCHASE_ID_KEY)));
    }

    private Optional<UUID> topLevelCreatedPurchaseId(Map<String, Object> metadata) {
        return ACTION_STATUS_EXECUTED.equals(value(metadata.get(ACTION_STATUS_KEY)))
                ? uuid(metadata.get(PURCHASE_ID_KEY))
                : Optional.empty();
    }

    private Optional<Map<String, Object>> action(Map<String, Object> metadata) {
        Object action = metadata.get(ACTION_KEY);
        return action instanceof Map<?, ?> actionMap
                ? Optional.of(castMap(actionMap))
                : Optional.empty();
    }

    private boolean isExecutedAdvancePurchaseAction(Map<String, Object> action) {
        return SchoolAssistantActionType.COMPRA_ANTICIPADA.name().equals(
                value(action.get("tipo")))
                && ACTION_STATUS_EXECUTED.equals(value(action.get("estado")));
    }

    private boolean isAssistantMessage(ChatMessage message) {
        return message.role() == RolChatMensaje.ASISTENTE_IA;
    }

    private String failureMessage(RuntimeException exception) {
        if (exception instanceof PurchaseAccessDeniedException) {
            return "Esa compra no corresponde a tu usuario.";
        }

        if (exception instanceof InvalidPurchaseStatusTransitionException) {
            return "Esa compra ya no se puede cancelar desde el asistente.";
        }

        if (isCancellationWindowMessage(exception)) {
            return exception.getMessage();
        }

        return "No pude cancelar esa compra.";
    }

    private boolean isCancellationWindowMessage(RuntimeException exception) {
        return exception.getMessage() != null
                && exception.getMessage().startsWith("No se puede cancelar");
    }

    private Optional<UUID> uuid(Object value) {
        try {
            return Optional.of(UUID.fromString(value(value)));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
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
}
