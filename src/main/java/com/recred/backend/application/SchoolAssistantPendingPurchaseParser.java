package com.recred.backend.application;

import com.recred.backend.domain.models.ChatMessage;
import com.recred.backend.domain.models.RolChatMensaje;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class SchoolAssistantPendingPurchaseParser {

    private static final String ACTION_STATUS_KEY = "estadoAccionChatbot";
    private static final String PENDING_ACTION_KEY = "accionPendienteChatbot";
    private static final String ACTION_TYPE_QUICK_PURCHASE = "COMPRA_RAPIDA";
    private static final String ACTION_STATUS_EXECUTED = "EJECUTADA";
    private static final String ACTION_STATUS_CANCELLED = "CANCELADA";
    private static final String ACTION_STATUS_FAILED = "FALLIDA";

    private final SchoolAssistantIntentDetector intentDetector;

    public SchoolAssistantPendingPurchaseParser(SchoolAssistantIntentDetector intentDetector) {
        this.intentDetector = intentDetector;
    }

    Optional<PendingPurchase> pendingPurchase(List<ChatMessage> recentMessages) {
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
                        localDate(action.get("fechaRetiro")),
                        intentDetector.recessTime(value(action.get("recreo")))
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

    private Optional<LocalDate> localDate(Object value) {
        try {
            return Optional.of(LocalDate.parse(value(value)));
        } catch (DateTimeParseException exception) {
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
