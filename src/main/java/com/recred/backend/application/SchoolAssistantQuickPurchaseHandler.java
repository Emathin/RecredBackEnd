package com.recred.backend.application;

import com.recred.backend.domain.models.ChatMessage;
import com.recred.backend.domain.models.RecessTime;
import com.recred.backend.domain.models.SchoolAssistantContext;
import com.recred.backend.domain.models.SchoolAssistantIntent;
import com.recred.backend.domain.models.TipoPlan;
import com.recred.backend.domain.repositories.TutorRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class SchoolAssistantQuickPurchaseHandler {

    private final SchoolAssistantPurchasePatternService purchasePatternService;
    private final TutorRepository tutorRepository;
    private final Clock clock;
    private final SchoolAssistantQuickPurchaseReplyFactory replyFactory;
    private final SchoolAssistantPendingPurchaseResponder pendingPurchaseResponder;

    public SchoolAssistantQuickPurchaseHandler(
            SchoolAssistantPurchasePatternService purchasePatternService,
            TutorRepository tutorRepository,
            Clock clock,
            SchoolAssistantQuickPurchaseReplyFactory replyFactory,
            SchoolAssistantPendingPurchaseResponder pendingPurchaseResponder
    ) {
        this.purchasePatternService = purchasePatternService;
        this.tutorRepository = tutorRepository;
        this.clock = clock;
        this.replyFactory = replyFactory;
        this.pendingPurchaseResponder = pendingPurchaseResponder;
    }

    SchoolAssistantFixedReply repeatLastPurchaseReply(
            SchoolAssistantContext context,
            Optional<LocalDate> selectedPurchaseDate
    ) {
        Optional<SchoolAssistantFixedReply> denied = advancedPlanRequiredReply(
                context,
                SchoolAssistantIntent.REPEAT_LAST_PURCHASE
        );
        if (denied.isPresent()) {
            return denied.get();
        }

        return purchasePatternService.lastRebuyablePlan(context.studentId())
                .map(plan -> purchasePlanReply(
                        plan,
                        SchoolAssistantIntent.REPEAT_LAST_PURCHASE,
                        "Puedo repetir tu ultima compra con estos productos",
                        selectedPurchaseDate
                ))
                .orElseGet(() -> replyFactory.actionFailureReply(
                        "Todavia no encontre una compra tuya que pueda repetir.",
                        SchoolAssistantIntent.REPEAT_LAST_PURCHASE
                ));
    }

    SchoolAssistantFixedReply buyUsualPurchaseReply(
            SchoolAssistantContext context,
            Optional<LocalDate> selectedPurchaseDate
    ) {
        Optional<SchoolAssistantFixedReply> denied = advancedPlanRequiredReply(
                context,
                SchoolAssistantIntent.BUY_USUAL
        );
        if (denied.isPresent()) {
            return denied.get();
        }

        return purchasePatternService.mostFrequentPlan(context.studentId())
                .map(plan -> purchasePlanReply(
                        plan,
                        SchoolAssistantIntent.BUY_USUAL,
                        "Detecte esta compra como habitual",
                        selectedPurchaseDate
                ))
                .orElseGet(() -> replyFactory.actionFailureReply(
                        "Todavia no tengo suficientes compras tuyas para armar "
                                + "una compra habitual.",
                        SchoolAssistantIntent.BUY_USUAL
                ));
    }

    SchoolAssistantFixedReply confirmPurchaseReply(
            SchoolAssistantContext context,
            List<ChatMessage> recentMessages
    ) {
        Optional<SchoolAssistantFixedReply> denied = advancedPlanRequiredReply(
                context,
                SchoolAssistantIntent.CONFIRM_PURCHASE
        );
        if (denied.isPresent()) {
            return denied.get();
        }

        return pendingPurchaseResponder.confirmPurchaseReply(context, recentMessages);
    }

    SchoolAssistantFixedReply selectPurchaseDateReply(
            SchoolAssistantContext context,
            List<ChatMessage> recentMessages,
            LocalDate pickupDate
    ) {
        Optional<SchoolAssistantFixedReply> denied = advancedPlanRequiredReply(
                context,
                SchoolAssistantIntent.SELECT_PURCHASE_DATE
        );
        if (denied.isPresent()) {
            return denied.get();
        }

        return pendingPurchaseResponder.selectPurchaseDateReply(
                context,
                recentMessages,
                pickupDate
        );
    }

    SchoolAssistantFixedReply selectRecessReply(
            SchoolAssistantContext context,
            List<ChatMessage> recentMessages,
            RecessTime recessTime
    ) {
        Optional<SchoolAssistantFixedReply> denied = advancedPlanRequiredReply(
                context,
                SchoolAssistantIntent.SELECT_RECESS
        );
        if (denied.isPresent()) {
            return denied.get();
        }

        return pendingPurchaseResponder.selectRecessReply(context, recentMessages, recessTime);
    }

    SchoolAssistantFixedReply cancelActionReply(List<ChatMessage> recentMessages) {
        return pendingPurchaseResponder.cancelActionReply(recentMessages);
    }

    boolean hasPendingPurchase(List<ChatMessage> recentMessages) {
        return pendingPurchaseResponder.hasPendingPurchase(recentMessages);
    }

    private SchoolAssistantFixedReply purchasePlanReply(
            PurchasePlan plan,
            SchoolAssistantIntent intent,
            String prefix,
            Optional<LocalDate> selectedPurchaseDate
    ) {
        if (selectedPurchaseDate.filter(this::isPastPickupDate).isPresent()) {
            return replyFactory.failedPurchaseReply(
                    "No puedo programar una compra para una fecha pasada.",
                    intent
            );
        }

        PendingPurchase pendingPurchase = new PendingPurchase(
                plan.studentId(),
                plan.buffetId(),
                plan.items().stream()
                        .map(item -> new PurchaseItemRequest(
                                item.productId(),
                                item.quantity(),
                                plan.buffetId()
                        ))
                        .toList(),
                selectedPurchaseDate,
                Optional.empty()
        );
        return replyFactory.pendingPurchaseReply(plan, intent, prefix, pendingPurchase);
    }

    private Optional<SchoolAssistantFixedReply> advancedPlanRequiredReply(
            SchoolAssistantContext context,
            SchoolAssistantIntent intent
    ) {
        if (studentHasAdvancedTutor(context.studentId())) {
            return Optional.empty();
        }

        return Optional.of(replyFactory.actionFailureReply(
                "Esta funcionalidad requiere plan AVANZADO.",
                intent
        ));
    }

    private boolean studentHasAdvancedTutor(UUID studentId) {
        return tutorRepository.findByAlumnoId(studentId)
                .stream()
                .filter(tutor -> tutor.getUsuario() != null)
                .anyMatch(tutor -> tutor.getUsuario().getPlan().atLeast(TipoPlan.AVANZADO));
    }

    private boolean isPastPickupDate(LocalDate pickupDate) {
        return pickupDate.isBefore(LocalDate.now(clock));
    }
}
