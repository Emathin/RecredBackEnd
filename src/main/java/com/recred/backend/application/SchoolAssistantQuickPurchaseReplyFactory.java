package com.recred.backend.application;

import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.RecessTime;
import com.recred.backend.domain.models.SchoolAssistantAction;
import com.recred.backend.domain.models.SchoolAssistantActionStatus;
import com.recred.backend.domain.models.SchoolAssistantActionType;
import com.recred.backend.domain.models.SchoolAssistantIntent;
import com.recred.backend.domain.models.SchoolAssistantRequiredInput;
import com.recred.backend.domain.models.SchoolAssistantRole;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class SchoolAssistantQuickPurchaseReplyFactory {

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

    private final SchoolAssistantReplyFormatter replyFormatter;
    private final SchoolAssistantContentFormatter contentFormatter;
    private final SchoolAssistantSuggestionFactory suggestionFactory;
    private final SchoolAssistantReplyBuilder replyBuilder;

    SchoolAssistantQuickPurchaseReplyFactory(
            SchoolAssistantReplyFormatter replyFormatter,
            SchoolAssistantContentFormatter contentFormatter,
            SchoolAssistantSuggestionFactory suggestionFactory,
            SchoolAssistantReplyBuilder replyBuilder
    ) {
        this.replyFormatter = replyFormatter;
        this.contentFormatter = contentFormatter;
        this.suggestionFactory = suggestionFactory;
        this.replyBuilder = replyBuilder;
    }

    SchoolAssistantFixedReply actionFailureReply(
            String answer,
            SchoolAssistantIntent intent
    ) {
        return replyBuilder.reply(answer, intent, replyBuilder.failedAdvanceAction(Map.of()));
    }

    SchoolAssistantFixedReply cancelActionReply(boolean hasPendingPurchase) {
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

    SchoolAssistantFixedReply dateSelectedReply(PendingPurchase pendingPurchase) {
        Map<String, Object> metadata = pendingMetadata(
                SchoolAssistantIntent.SELECT_PURCHASE_DATE.name(),
                pendingPurchase
        );
        Map<String, Object> data = contentFormatter.pendingPurchaseData(pendingPurchase);
        SchoolAssistantAction action = advanceAction(
                SchoolAssistantActionStatus.ESPERANDO_RECREO,
                true,
                List.of(SchoolAssistantRequiredInput.RECREO),
                data,
                null
        );

        return new SchoolAssistantFixedReply(
                "Listo, la dejo para el "
                        + contentFormatter.date(pendingPurchase.pickupDate().orElseThrow())
                        + ".\nAhora elegi el recreo de retiro:\n"
                        + replyFormatter.formattedItems(RECESS_OPTIONS),
                metadata,
                SchoolAssistantIntent.SELECT_PURCHASE_DATE,
                action,
                suggestionFactory.recessSuggestions(),
                data
        );
    }

    SchoolAssistantFixedReply confirmationReply(
            PendingPurchase pendingPurchase,
            SchoolAssistantIntent intent
    ) {
        Map<String, Object> metadata = pendingMetadata(intent.name(), pendingPurchase);
        Map<String, Object> data = contentFormatter.pendingPurchaseData(pendingPurchase);
        SchoolAssistantAction action = advanceAction(
                SchoolAssistantActionStatus.ESPERANDO_CONFIRMACION,
                true,
                List.of(SchoolAssistantRequiredInput.CONFIRMACION),
                data,
                pendingPurchase.recessTime().orElseThrow()
        );

        return new SchoolAssistantFixedReply(
                "Listo, la dejo para el "
                        + contentFormatter.date(pendingPurchase.pickupDate().orElseThrow())
                        + " en "
                        + contentFormatter.recessLabel(pendingPurchase.recessTime().orElseThrow())
                        + ".\nEscribi confirmar para crear la compra anticipada o cancelar.",
                metadata,
                intent,
                action,
                suggestionFactory.confirmationSuggestions(),
                data
        );
    }

    SchoolAssistantFixedReply executedPurchaseReply(
            Purchase purchase,
            LocalDate pickupDate,
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
                        + "\n- Fecha de retiro: "
                        + contentFormatter.date(pickupDate)
                        + "\n- Estado: "
                        + contentFormatter.purchaseStatusLabel(purchase.getStatus())
                        + "\n- Codigo de retiro: "
                        + contentFormatter.value(purchase.getWithdrawalCode()) + ".",
                metadata,
                SchoolAssistantIntent.CONFIRM_PURCHASE,
                action,
                suggestionFactory.suggestions(
                        SchoolAssistantRole.ALUMNO,
                        SchoolAssistantIntent.CONFIRM_PURCHASE,
                        action
                ),
                data
        );
    }

    SchoolAssistantFixedReply missingDateReply(PendingPurchase pendingPurchase) {
        Map<String, Object> data = contentFormatter.pendingPurchaseData(pendingPurchase);
        return new SchoolAssistantFixedReply(
                dateSelectionText(),
                pendingMetadata(
                        SchoolAssistantIntent.CONFIRM_PURCHASE.name(),
                        pendingPurchase
                ),
                SchoolAssistantIntent.CONFIRM_PURCHASE,
                advanceAction(
                        SchoolAssistantActionStatus.ESPERANDO_FECHA,
                        true,
                        List.of(SchoolAssistantRequiredInput.FECHA_RETIRO),
                        data,
                        pendingPurchase.recessTime().orElse(null)
                ),
                suggestionFactory.dateSuggestions(),
                data
        );
    }

    SchoolAssistantFixedReply missingRecessReply(PendingPurchase pendingPurchase) {
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

    SchoolAssistantFixedReply failedPurchaseReply(String answer) {
        return failedPurchaseReply(answer, SchoolAssistantIntent.CONFIRM_PURCHASE);
    }

    SchoolAssistantFixedReply failedPurchaseReply(
            String answer,
            SchoolAssistantIntent intent
    ) {
        Map<String, Object> metadata = metadata(
                intent.name()
        );
        metadata.put(ACTION_STATUS_KEY, ACTION_STATUS_FAILED);
        return new SchoolAssistantFixedReply(
                answer,
                metadata,
                intent,
                replyBuilder.failedAdvanceAction(Map.of()),
                suggestionFactory.quickPurchaseSuggestions(),
                Map.of()
        );
    }

    SchoolAssistantFixedReply pendingPurchaseReply(
            PurchasePlan plan,
            SchoolAssistantIntent intent,
            String prefix,
            PendingPurchase pendingPurchase
    ) {
        Map<String, Object> metadata = pendingMetadata(intent.name(), pendingPurchase);
        Map<String, Object> data = contentFormatter.pendingPurchaseData(pendingPurchase);
        data.put("totalEstimado", plan.totalAmount());
        data.put("ocurrencias", plan.occurrences());
        data.put("ultimaFecha", contentFormatter.value(plan.lastDate()));

        if (pendingPurchase.pickupDate().isPresent()) {
            return pendingPurchaseWithDateReply(prefix, plan, pendingPurchase, metadata, data);
        }

        return new SchoolAssistantFixedReply(
                prefix + ":\n" + contentFormatter.purchasePlanItemList(plan.items())
                        + "\n\nTotal estimado: "
                        + contentFormatter.money(plan.totalAmount())
                        + "\n" + dateSelectionText(),
                metadata,
                intent,
                advanceAction(
                        SchoolAssistantActionStatus.ESPERANDO_FECHA,
                        true,
                        List.of(SchoolAssistantRequiredInput.FECHA_RETIRO),
                        data,
                        null
                ),
                suggestionFactory.dateSuggestions(),
                data
        );
    }

    private SchoolAssistantFixedReply pendingPurchaseWithDateReply(
            String prefix,
            PurchasePlan plan,
            PendingPurchase pendingPurchase,
            Map<String, Object> metadata,
            Map<String, Object> data
    ) {
        return new SchoolAssistantFixedReply(
                prefix + ":\n" + contentFormatter.purchasePlanItemList(plan.items())
                        + "\n\nTotal estimado: "
                        + contentFormatter.money(plan.totalAmount())
                        + "\nListo, la dejo para el "
                        + contentFormatter.date(pendingPurchase.pickupDate().orElseThrow())
                        + ".\nAhora elegi el recreo de retiro:\n"
                        + replyFormatter.formattedItems(RECESS_OPTIONS),
                metadata,
                SchoolAssistantIntent.SELECT_PURCHASE_DATE,
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

    private String dateSelectionText() {
        return "Para terminar, elegi el dia de retiro.";
    }

    private String recessSelectionText() {
        return "Para terminar, elegi el recreo de retiro:\n"
                + replyFormatter.formattedItems(RECESS_OPTIONS);
    }

    private Map<String, Object> pendingAction(PendingPurchase pendingPurchase) {
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("tipo", ACTION_TYPE_QUICK_PURCHASE);
        action.put("alumnoId", pendingPurchase.studentId().toString());
        action.put("buffetId", pendingPurchase.buffetId().toString());
        pendingPurchase.pickupDate().ifPresent(pickupDate ->
                action.put("fechaRetiro", pickupDate.toString()));
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

    private Map<String, Object> metadata(String intent) {
        return replyBuilder.metadata(intent);
    }

    private Map<String, Object> pendingMetadata(String intent, PendingPurchase pendingPurchase) {
        Map<String, Object> metadata = metadata(intent);
        metadata.put(ACTION_STATUS_KEY, ACTION_STATUS_PENDING);
        metadata.put(PENDING_ACTION_KEY, pendingAction(pendingPurchase));
        return metadata;
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
}
