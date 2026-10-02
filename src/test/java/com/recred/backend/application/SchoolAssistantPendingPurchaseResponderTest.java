package com.recred.backend.application;

import com.recred.backend.domain.models.ChatMessage;
import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.RecessTime;
import com.recred.backend.domain.models.RolChatMensaje;
import com.recred.backend.domain.models.SchoolAssistantActionStatus;
import com.recred.backend.domain.models.SchoolAssistantActionType;
import com.recred.backend.domain.models.SchoolAssistantContext;
import com.recred.backend.domain.models.SchoolAssistantIntent;
import com.recred.backend.domain.models.SchoolAssistantRequiredInput;
import com.recred.backend.domain.repositories.ProductRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchoolAssistantPendingPurchaseResponderTest {

    private static final LocalDate PICKUP_DATE = LocalDate.of(2026, 5, 28);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 27, 12, 0);

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CreateAssistantAdvancePurchase createAssistantAdvancePurchase;

    private SchoolAssistantPendingPurchaseResponder responder;
    private UUID studentId;
    private UUID sessionId;
    private UUID buffetId;
    private UUID purchaseId;
    private UUID pickupSlotId;
    private Producto product;
    private SchoolAssistantContext context;
    private List<ChatMessage> recentMessages;
    private SchoolAssistantFixedReply reply;

    @BeforeEach
    void setUp() {
        SchoolAssistantReplyFormatter replyFormatter = new SchoolAssistantReplyFormatter();
        SchoolAssistantContentFormatter contentFormatter =
                new SchoolAssistantContentFormatter(productRepository, replyFormatter);
        SchoolAssistantSuggestionFactory suggestionFactory =
                new SchoolAssistantSuggestionFactory();
        SchoolAssistantReplyBuilder replyBuilder =
                new SchoolAssistantReplyBuilder(suggestionFactory);
        SchoolAssistantQuickPurchaseReplyFactory replyFactory =
                new SchoolAssistantQuickPurchaseReplyFactory(
                        replyFormatter,
                        contentFormatter,
                        suggestionFactory,
                        replyBuilder
                );
        responder = new SchoolAssistantPendingPurchaseResponder(
                productRepository,
                createAssistantAdvancePurchase,
                fixedClock(),
                new SchoolAssistantPurchaseFailureMessages(),
                new SchoolAssistantPendingPurchaseParser(new SchoolAssistantIntentDetector()),
                replyFactory
        );
        studentId = UUID.randomUUID();
        sessionId = UUID.randomUUID();
        buffetId = UUID.randomUUID();
        purchaseId = UUID.randomUUID();
        pickupSlotId = UUID.randomUUID();
        product = product();
        recentMessages = List.of();
    }

    @Test
    void confirmPurchaseReply_WhenPendingPurchaseHasDateAndRecess_ShouldExecutePurchase() {
        givenStudentContext();
        givenPendingPurchaseReadyToConfirm();
        givenActiveProduct();
        givenAdvancePurchaseIsCreated();

        whenConfirmingPendingPurchase();

        thenPendingPurchaseWasExecuted();
    }

    @Test
    void selectPurchaseDateReply_WhenPendingDraftExists_ShouldAskForRecess() {
        givenStudentContext();
        givenPendingPurchaseWithoutDate();

        whenSelectingPurchaseDate();

        thenSelectedDateWasStoredAndRecessWasRequested();
    }

    @Test
    void selectRecessReply_WhenPendingDraftHasDate_ShouldAskForConfirmation() {
        givenStudentContext();
        givenPendingPurchaseWithDate();

        whenSelectingRecess();

        thenSelectedRecessWasStoredAndConfirmationWasRequested();
    }

    @Test
    void cancelActionReply_WhenPendingDraftExists_ShouldDiscardDraft() {
        givenStudentContext();
        givenPendingPurchaseWithoutDate();

        whenCancellingPendingAction();

        thenPendingDraftWasDiscarded();
    }

    @Test
    void confirmPurchaseReply_WhenPendingDraftBelongsToAnotherStudent_ShouldFail() {
        givenStudentContext();
        givenPendingPurchaseFromAnotherStudent();

        whenConfirmingPendingPurchase();

        thenPendingPurchaseOwnershipWasRejected();
    }

    private void givenStudentContext() {
        context = new SchoolAssistantContext(
                studentId,
                Optional.empty(),
                List.of(),
                List.of(),
                List.of()
        );
    }

    private void givenPendingPurchaseReadyToConfirm() {
        recentMessages = List.of(assistantMessage(
                pendingMetadata(studentId, Optional.of(PICKUP_DATE),
                        Optional.of(RecessTime.FIRST_RECESS))
        ));
    }

    private void givenPendingPurchaseWithoutDate() {
        recentMessages = List.of(assistantMessage(
                pendingMetadata(studentId, Optional.empty(), Optional.empty())
        ));
    }

    private void givenPendingPurchaseWithDate() {
        recentMessages = List.of(assistantMessage(
                pendingMetadata(studentId, Optional.of(PICKUP_DATE), Optional.empty())
        ));
    }

    private void givenPendingPurchaseFromAnotherStudent() {
        recentMessages = List.of(assistantMessage(
                pendingMetadata(UUID.randomUUID(), Optional.of(PICKUP_DATE),
                        Optional.of(RecessTime.FIRST_RECESS))
        ));
    }

    private void givenActiveProduct() {
        when(productRepository.findActiveById(product.getId())).thenReturn(Optional.of(product));
    }

    private void givenAdvancePurchaseIsCreated() {
        when(createAssistantAdvancePurchase.execute(any()))
                .thenReturn(createdAdvancePurchase());
    }

    private void whenConfirmingPendingPurchase() {
        reply = responder.confirmPurchaseReply(context, recentMessages);
    }

    private void whenSelectingPurchaseDate() {
        reply = responder.selectPurchaseDateReply(context, recentMessages, PICKUP_DATE);
    }

    private void whenSelectingRecess() {
        reply = responder.selectRecessReply(
                context,
                recentMessages,
                RecessTime.FIRST_RECESS
        );
    }

    private void whenCancellingPendingAction() {
        reply = responder.cancelActionReply(recentMessages);
    }

    private void thenPendingPurchaseWasExecuted() {
        assertTrue(reply.answer().contains("tu compra anticipada quedo creada"));
        assertEquals(SchoolAssistantIntent.CONFIRM_PURCHASE, reply.intent());
        assertEquals(SchoolAssistantActionType.COMPRA_ANTICIPADA, reply.action().type());
        assertEquals(SchoolAssistantActionStatus.EJECUTADA, reply.action().status());
        assertEquals(purchaseId, reply.action().purchaseId());
        assertEquals("EJECUTADA", reply.metadata().get("estadoAccionChatbot"));
        verify(createAssistantAdvancePurchase)
                .execute(argThat(this::isExpectedAdvancePurchaseRequest));
    }

    private void thenSelectedDateWasStoredAndRecessWasRequested() {
        assertEquals(SchoolAssistantIntent.SELECT_PURCHASE_DATE, reply.intent());
        assertEquals(SchoolAssistantActionStatus.ESPERANDO_RECREO, reply.action().status());
        assertEquals(List.of(SchoolAssistantRequiredInput.RECREO),
                reply.action().requiredInputs());
        assertEquals(PICKUP_DATE.toString(), pendingAction().get("fechaRetiro"));
        assertEquals("PENDIENTE", reply.metadata().get("estadoAccionChatbot"));
    }

    private void thenSelectedRecessWasStoredAndConfirmationWasRequested() {
        assertEquals(SchoolAssistantIntent.SELECT_RECESS, reply.intent());
        assertEquals(SchoolAssistantActionStatus.ESPERANDO_CONFIRMACION,
                reply.action().status());
        assertEquals(List.of(SchoolAssistantRequiredInput.CONFIRMACION),
                reply.action().requiredInputs());
        assertEquals(RecessTime.FIRST_RECESS, reply.action().recessTime());
        assertEquals(RecessTime.FIRST_RECESS.name(), pendingAction().get("recreo"));
    }

    private void thenPendingDraftWasDiscarded() {
        assertEquals("Listo, descarte esa compra pendiente.", reply.answer());
        assertEquals(SchoolAssistantIntent.CANCEL_ACTION, reply.intent());
        assertEquals(SchoolAssistantActionStatus.CANCELADA, reply.action().status());
        assertEquals("CANCELADA", reply.metadata().get("estadoAccionChatbot"));
        verifyNoInteractions(productRepository, createAssistantAdvancePurchase);
    }

    private void thenPendingPurchaseOwnershipWasRejected() {
        assertEquals("Esa compra pendiente no corresponde a tu usuario.", reply.answer());
        assertEquals(SchoolAssistantIntent.CONFIRM_PURCHASE, reply.intent());
        assertEquals(SchoolAssistantActionStatus.FALLIDA, reply.action().status());
        verifyNoInteractions(productRepository, createAssistantAdvancePurchase);
    }

    private boolean isExpectedAdvancePurchaseRequest(
            CreateAssistantAdvancePurchaseRequest request
    ) {
        return request.studentId().equals(studentId)
                && request.buffetId().equals(buffetId)
                && request.pickupDate().equals(PICKUP_DATE)
                && request.recessTime().equals(RecessTime.FIRST_RECESS)
                && request.cart().get(product).equals(2);
    }

    private Map<?, ?> pendingAction() {
        return (Map<?, ?>) reply.metadata().get("accionPendienteChatbot");
    }

    private ChatMessage assistantMessage(Map<String, Object> metadata) {
        return new ChatMessage(
                sessionId,
                RolChatMensaje.ASISTENTE_IA,
                "draft",
                metadata,
                NOW
        );
    }

    private Map<String, Object> pendingMetadata(
            UUID ownerId,
            Optional<LocalDate> pickupDate,
            Optional<RecessTime> recessTime
    ) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("estadoAccionChatbot", "PENDIENTE");
        metadata.put("accionPendienteChatbot", pendingAction(ownerId, pickupDate, recessTime));
        return metadata;
    }

    private Map<String, Object> pendingAction(
            UUID ownerId,
            Optional<LocalDate> pickupDate,
            Optional<RecessTime> recessTime
    ) {
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("tipo", "COMPRA_RAPIDA");
        action.put("alumnoId", ownerId.toString());
        action.put("buffetId", buffetId.toString());
        action.put("items", List.of(pendingItem()));
        pickupDate.ifPresent(date -> action.put("fechaRetiro", date.toString()));
        recessTime.ifPresent(recess -> action.put("recreo", recess.name()));
        return action;
    }

    private Map<String, Object> pendingItem() {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("productoId", product.getId().toString());
        item.put("cantidad", 2);
        return item;
    }

    private Producto product() {
        Producto product = new Producto();
        product.setId(UUID.randomUUID());
        product.setNombre("Yogur");
        product.setPrecio(new BigDecimal("900.00"));
        product.setRequierePreparacion(false);
        return product;
    }

    private CreateAssistantAdvancePurchaseResponse createdAdvancePurchase() {
        return new CreateAssistantAdvancePurchaseResponse(
                purchaseId,
                studentId,
                buffetId,
                EstadoCompra.PENDIENTE,
                "RET-123",
                new BigDecimal("1800.00"),
                PICKUP_DATE,
                pickupSlotId,
                NOW,
                List.of(createdAdvancePurchaseItem())
        );
    }

    private CreateAssistantAdvancePurchaseResponse.Item createdAdvancePurchaseItem() {
        return new CreateAssistantAdvancePurchaseResponse.Item(
                product.getId(),
                product.getNombre(),
                2,
                product.getPrecio()
        );
    }

    private Clock fixedClock() {
        return Clock.fixed(
                Instant.parse("2026-05-27T12:00:00Z"),
                ZoneId.of("America/Argentina/Buenos_Aires")
        );
    }
}

