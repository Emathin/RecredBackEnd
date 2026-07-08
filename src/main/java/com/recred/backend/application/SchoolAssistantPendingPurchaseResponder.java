package com.recred.backend.application;

import com.recred.backend.domain.exceptions.InsufficientBalanceException;
import com.recred.backend.domain.exceptions.PickupTimeSlotNotFoundException;
import com.recred.backend.domain.exceptions.PresupuestoExcedidoException;
import com.recred.backend.domain.exceptions.RestriccionHorariaException;
import com.recred.backend.domain.exceptions.RestriccionSaludException;
import com.recred.backend.domain.models.ChatMessage;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.RecessTime;
import com.recred.backend.domain.models.SchoolAssistantContext;
import com.recred.backend.domain.models.SchoolAssistantIntent;
import com.recred.backend.domain.repositories.ProductRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class SchoolAssistantPendingPurchaseResponder {

    private final ProductRepository productRepository;
    private final CreateAssistantAdvancePurchase createAssistantAdvancePurchase;
    private final Clock clock;
    private final SchoolAssistantPurchaseFailureMessages purchaseFailureMessages;
    private final SchoolAssistantPendingPurchaseParser pendingPurchaseParser;
    private final SchoolAssistantQuickPurchaseReplyFactory replyFactory;

    public SchoolAssistantPendingPurchaseResponder(
            ProductRepository productRepository,
            CreateAssistantAdvancePurchase createAssistantAdvancePurchase,
            Clock clock,
            SchoolAssistantPurchaseFailureMessages purchaseFailureMessages,
            SchoolAssistantPendingPurchaseParser pendingPurchaseParser,
            SchoolAssistantQuickPurchaseReplyFactory replyFactory
    ) {
        this.productRepository = productRepository;
        this.createAssistantAdvancePurchase = createAssistantAdvancePurchase;
        this.clock = clock;
        this.purchaseFailureMessages = purchaseFailureMessages;
        this.pendingPurchaseParser = pendingPurchaseParser;
        this.replyFactory = replyFactory;
    }

    SchoolAssistantFixedReply confirmPurchaseReply(
            SchoolAssistantContext context,
            List<ChatMessage> recentMessages
    ) {
        return pendingPurchaseParser.pendingPurchase(recentMessages)
                .map(pendingPurchase -> executePendingPurchase(context, pendingPurchase))
                .orElseGet(() -> replyFactory.actionFailureReply(
                        "No tengo ninguna compra pendiente para confirmar.",
                        SchoolAssistantIntent.CONFIRM_PURCHASE
                ));
    }

    SchoolAssistantFixedReply selectPurchaseDateReply(
            SchoolAssistantContext context,
            List<ChatMessage> recentMessages,
            LocalDate pickupDate
    ) {
        return pendingPurchaseParser.pendingPurchase(recentMessages)
                .map(pendingPurchase -> withSelectedPurchaseDateReply(
                        context,
                        pendingPurchase,
                        pickupDate
                ))
                .orElseGet(() -> replyFactory.actionFailureReply(
                        "No tengo ninguna compra pendiente para elegir dia.",
                        SchoolAssistantIntent.SELECT_PURCHASE_DATE
                ));
    }

    SchoolAssistantFixedReply selectRecessReply(
            SchoolAssistantContext context,
            List<ChatMessage> recentMessages,
            RecessTime recessTime
    ) {
        return pendingPurchaseParser.pendingPurchase(recentMessages)
                .map(pendingPurchase -> withSelectedRecessReply(
                        context,
                        pendingPurchase,
                        recessTime
                ))
                .orElseGet(() -> replyFactory.actionFailureReply(
                        "No tengo ninguna compra pendiente para elegir recreo.",
                        SchoolAssistantIntent.SELECT_RECESS
                ));
    }

    SchoolAssistantFixedReply cancelActionReply(List<ChatMessage> recentMessages) {
        boolean hasPendingPurchase = hasPendingPurchase(recentMessages);
        return replyFactory.cancelActionReply(hasPendingPurchase);
    }

    boolean hasPendingPurchase(List<ChatMessage> recentMessages) {
        return pendingPurchaseParser.pendingPurchase(recentMessages).isPresent();
    }

    private SchoolAssistantFixedReply executePendingPurchase(
            SchoolAssistantContext context,
            PendingPurchase pendingPurchase
    ) {
        UUID studentId = context.studentId();

        if (studentDoesNotOwnPendingPurchase(studentId, pendingPurchase)) {
            return replyFactory.failedPurchaseReply(
                    "Esa compra pendiente no corresponde a tu usuario."
            );
        }

        if (pendingPurchase.pickupDate().isEmpty()) {
            return replyFactory.missingDateReply(pendingPurchase);
        }

        if (isPastPickupDate(pendingPurchase.pickupDate().orElseThrow())) {
            return replyFactory.failedPurchaseReply(
                    "No puedo programar una compra para una fecha pasada."
            );
        }

        if (pendingPurchase.recessTime().isEmpty()) {
            return replyFactory.missingRecessReply(pendingPurchase);
        }

        Optional<Map<Producto, Integer>> cart = cart(pendingPurchase);

        if (cart.isEmpty()) {
            return replyFactory.failedPurchaseReply(
                    "No pude realizar la compra porque algun producto ya no esta "
                            + "disponible."
            );
        }

        try {
            CreateAssistantAdvancePurchaseRequest request =
                    new CreateAssistantAdvancePurchaseRequest(
                            studentId,
                            pendingPurchase.buffetId(),
                            cart.orElseThrow(),
                            pendingPurchase.recessTime().orElseThrow(),
                            pendingPurchase.pickupDate().orElseThrow()
                    );
            CreateAssistantAdvancePurchaseResponse purchase =
                    createAssistantAdvancePurchase.execute(request);
            return replyFactory.executedPurchaseReply(
                    purchase,
                    pendingPurchase.pickupDate().orElseThrow(),
                    pendingPurchase.recessTime().orElseThrow()
            );
        } catch (InsufficientBalanceException
                 | PickupTimeSlotNotFoundException
                 | PresupuestoExcedidoException
                 | RestriccionHorariaException
                 | RestriccionSaludException exception) {
            return replyFactory.failedPurchaseReply(purchaseFailureMessages.message(exception));
        }
    }

    private SchoolAssistantFixedReply withSelectedRecessReply(
            SchoolAssistantContext context,
            PendingPurchase pendingPurchase,
            RecessTime recessTime
    ) {
        if (studentDoesNotOwnPendingPurchase(context.studentId(), pendingPurchase)) {
            return replyFactory.failedPurchaseReply(
                    "Esa compra pendiente no corresponde a tu usuario."
            );
        }

        PendingPurchase updatedPurchase = new PendingPurchase(
                pendingPurchase.studentId(),
                pendingPurchase.buffetId(),
                pendingPurchase.items(),
                pendingPurchase.pickupDate(),
                Optional.of(recessTime)
        );

        if (updatedPurchase.pickupDate().isEmpty()) {
            return replyFactory.missingDateReply(updatedPurchase);
        }

        return replyFactory.confirmationReply(updatedPurchase, SchoolAssistantIntent.SELECT_RECESS);
    }

    private SchoolAssistantFixedReply withSelectedPurchaseDateReply(
            SchoolAssistantContext context,
            PendingPurchase pendingPurchase,
            LocalDate pickupDate
    ) {
        if (studentDoesNotOwnPendingPurchase(context.studentId(), pendingPurchase)) {
            return replyFactory.failedPurchaseReply(
                    "Esa compra pendiente no corresponde a tu usuario."
            );
        }

        if (isPastPickupDate(pickupDate)) {
            return replyFactory.failedPurchaseReply(
                    "No puedo programar una compra para una fecha pasada."
            );
        }

        PendingPurchase updatedPurchase = new PendingPurchase(
                pendingPurchase.studentId(),
                pendingPurchase.buffetId(),
                pendingPurchase.items(),
                Optional.of(pickupDate),
                pendingPurchase.recessTime()
        );

        return updatedPurchase.recessTime().isPresent()
                ? replyFactory.confirmationReply(
                        updatedPurchase,
                        SchoolAssistantIntent.SELECT_PURCHASE_DATE
                )
                : replyFactory.dateSelectedReply(updatedPurchase);
    }

    private Optional<Map<Producto, Integer>> cart(PendingPurchase pendingPurchase) {
        Map<Producto, Integer> cart = new LinkedHashMap<>();
        pendingPurchase.items().forEach(item -> productRepository.findActiveById(item.productId())
                .ifPresent(product -> cart.put(product, item.quantity())));

        return cart.size() == pendingPurchase.items().size()
                ? Optional.of(cart)
                : Optional.empty();
    }

    private boolean studentDoesNotOwnPendingPurchase(
            UUID studentId,
            PendingPurchase pendingPurchase
    ) {
        return !pendingPurchase.studentId().equals(studentId);
    }

    private boolean isPastPickupDate(LocalDate pickupDate) {
        return pickupDate.isBefore(LocalDate.now(clock));
    }
}
