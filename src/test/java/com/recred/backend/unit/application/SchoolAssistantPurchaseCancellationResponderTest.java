package com.recred.backend.unit.application;

import com.recred.backend.application.CancelPresentialPurchase;
import com.recred.backend.application.CancelPresentialPurchaseRequest;
import com.recred.backend.application.CancelPresentialPurchaseResponse;
import com.recred.backend.application.SchoolAssistantContentFormatter;
import com.recred.backend.application.SchoolAssistantFixedReply;
import com.recred.backend.application.SchoolAssistantIntentDetector;
import com.recred.backend.application.SchoolAssistantPurchaseCancellationHandler;
import com.recred.backend.application.SchoolAssistantPurchaseCancellationResponder;
import com.recred.backend.application.SchoolAssistantReplyBuilder;
import com.recred.backend.application.SchoolAssistantReplyFormatter;
import com.recred.backend.application.SchoolAssistantSuggestionFactory;
import com.recred.backend.domain.models.ChatMessage;
import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.RolChatMensaje;
import com.recred.backend.domain.models.SchoolAssistantActionStatus;
import com.recred.backend.domain.models.SchoolAssistantActionType;
import com.recred.backend.domain.models.SchoolAssistantContext;
import com.recred.backend.domain.models.SchoolAssistantIntent;
import com.recred.backend.domain.models.SchoolAssistantRole;
import com.recred.backend.domain.repositories.ProductRepository;
import com.recred.backend.domain.repositories.PurchaseRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchoolAssistantPurchaseCancellationResponderTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 1, 10, 0);

    @Mock
    private PurchaseRepository purchaseRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CancelPresentialPurchase cancelPresentialPurchase;

    private SchoolAssistantPurchaseCancellationResponder responder;
    private UUID studentId;
    private UUID sessionId;
    private UUID buffetId;
    private UUID purchaseId;
    private SchoolAssistantContext context;
    private List<ChatMessage> recentMessages;
    private Optional<SchoolAssistantFixedReply> reply;

    @BeforeEach
    void setUp() {
        responder = purchaseCancellationResponder();
        studentId = UUID.randomUUID();
        sessionId = UUID.randomUUID();
        buffetId = UUID.randomUUID();
        purchaseId = UUID.randomUUID();
        recentMessages = List.of();
    }

    private SchoolAssistantPurchaseCancellationResponder purchaseCancellationResponder() {
        SchoolAssistantReplyFormatter replyFormatter = new SchoolAssistantReplyFormatter();
        SchoolAssistantContentFormatter contentFormatter =
                new SchoolAssistantContentFormatter(productRepository, replyFormatter);
        SchoolAssistantReplyBuilder replyBuilder = new SchoolAssistantReplyBuilder(
                new SchoolAssistantSuggestionFactory()
        );
        return new SchoolAssistantPurchaseCancellationResponder(
                new SchoolAssistantIntentDetector(),
                new SchoolAssistantPurchaseCancellationHandler(
                        purchaseRepository,
                        cancelPresentialPurchase,
                        contentFormatter,
                        replyBuilder
                )
        );
    }

    @Test
    void respond_WhenStudentCancelsRecentCreatedPurchase_ShouldCancelPurchase() {
        givenStudentContext();
        givenRecentCreatedPurchase();
        givenSuccessfulCancellation();

        whenRespondingTo("cancelar mi compra");

        thenPurchaseWasCancelled();
        thenCancellationUseCaseWasExecuted();
    }

    @Test
    void respond_WhenStudentCancelsWithoutCancelablePurchases_ShouldExplainMissingPurchase() {
        givenStudentContext();
        givenNoCancelablePurchaseHistory();

        whenRespondingTo("cancelar mi compra");

        thenMissingCancelablePurchaseReplyWasReturned();
        thenCancellationUseCaseWasNotExecuted();
    }

    @Test
    void respond_WhenStudentHasManyCancelablePurchases_ShouldAskToReviewOrders() {
        givenStudentContext();
        givenManyCancelablePurchases();

        whenRespondingTo("cancelar mi compra");

        thenMultipleCancelablePurchasesReplyWasReturned();
        thenCancellationUseCaseWasNotExecuted();
    }

    @Test
    void respond_WhenTutorCancelsPurchase_ShouldReturnEmpty() {
        givenTutorContext();

        whenRespondingTo("cancelar mi compra");

        thenNoCancellationReplyWasReturned();
        thenNoDependenciesWereUsed();
    }

    @Test
    void respond_WhenMessageIsNotPurchaseCancellation_ShouldReturnEmpty() {
        givenStudentContext();

        whenRespondingTo("cuanto saldo tengo");

        thenNoCancellationReplyWasReturned();
        thenNoDependenciesWereUsed();
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

    private void givenTutorContext() {
        context = new SchoolAssistantContext(
                null,
                Optional.empty(),
                List.of(),
                List.of(),
                List.of(),
                SchoolAssistantRole.TUTOR,
                UUID.randomUUID(),
                null,
                List.of(),
                List.of()
        );
    }

    private void givenRecentCreatedPurchase() {
        recentMessages = List.of(assistantMessage(createdPurchaseMetadata()));
    }

    private void givenSuccessfulCancellation() {
        when(cancelPresentialPurchase.execute(argThat(this::matchesCancellationRequest)))
                .thenReturn(cancelledResponse());
    }

    private void givenNoCancelablePurchaseHistory() {
        when(purchaseRepository.findHistorialByAlumno(studentId)).thenReturn(List.of(
                purchase(UUID.randomUUID(), EstadoCompra.ENTREGADO, 1)
        ));
    }

    private void givenManyCancelablePurchases() {
        when(purchaseRepository.findHistorialByAlumno(studentId)).thenReturn(List.of(
                purchase(UUID.randomUUID(), EstadoCompra.PENDIENTE, 1),
                purchase(UUID.randomUUID(), EstadoCompra.PENDIENTE, 2)
        ));
    }

    private void whenRespondingTo(String message) {
        reply = responder.respond(message, context, recentMessages);
    }

    private void thenPurchaseWasCancelled() {
        SchoolAssistantFixedReply answer = presentReply();
        assertEquals("Listo, cancele tu compra y te devolvi $1500.00 en creditos.",
                answer.answer());
        assertEquals(SchoolAssistantIntent.CANCEL_PURCHASE, answer.intent());
        assertEquals(SchoolAssistantActionType.CANCELACION_COMPRA, answer.action().type());
        assertEquals(SchoolAssistantActionStatus.EJECUTADA, answer.action().status());
        assertEquals(purchaseId, answer.action().purchaseId());
        assertEquals(EstadoCompra.CANCELADO, answer.action().purchaseStatus());
        assertEquals(new BigDecimal("1500.00"), answer.action().total());
    }

    private void thenCancellationUseCaseWasExecuted() {
        verify(cancelPresentialPurchase).execute(argThat(this::matchesCancellationRequest));
        verifyNoInteractions(purchaseRepository);
    }

    private void thenMissingCancelablePurchaseReplyWasReturned() {
        SchoolAssistantFixedReply answer = presentReply();
        assertEquals("No encontre compras pendientes que puedas cancelar.", answer.answer());
        assertEquals(SchoolAssistantIntent.CANCEL_PURCHASE, answer.intent());
        assertEquals(SchoolAssistantActionType.CANCELACION_COMPRA, answer.action().type());
        assertEquals(SchoolAssistantActionStatus.FALLIDA, answer.action().status());
    }

    private void thenMultipleCancelablePurchasesReplyWasReturned() {
        SchoolAssistantFixedReply answer = presentReply();
        assertTrue(answer.answer().contains("Tenes mas de una compra pendiente"));
        assertTrue(answer.answer().contains("1x Alfajor"));
        assertTrue(answer.answer().contains("2x Alfajor"));
        assertEquals(SchoolAssistantActionStatus.INFORMATIVA, answer.action().status());
    }

    private void thenCancellationUseCaseWasNotExecuted() {
        verifyNoInteractions(cancelPresentialPurchase);
    }

    private void thenNoCancellationReplyWasReturned() {
        assertFalse(reply.isPresent());
    }

    private void thenNoDependenciesWereUsed() {
        verifyNoInteractions(
                purchaseRepository,
                productRepository,
                cancelPresentialPurchase
        );
    }

    private SchoolAssistantFixedReply presentReply() {
        assertTrue(reply.isPresent());
        return reply.orElseThrow();
    }

    private boolean matchesCancellationRequest(CancelPresentialPurchaseRequest request) {
        return request != null
                && purchaseId.equals(request.purchaseId())
                && studentId.equals(request.studentId());
    }

    private ChatMessage assistantMessage(Map<String, Object> metadata) {
        return new ChatMessage(
                sessionId,
                RolChatMensaje.ASISTENTE_IA,
                "compra creada",
                metadata,
                NOW
        );
    }

    private Map<String, Object> createdPurchaseMetadata() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("estadoAccionChatbot", "EJECUTADA");
        metadata.put("compraId", purchaseId.toString());
        return metadata;
    }

    private CancelPresentialPurchaseResponse cancelledResponse() {
        return new CancelPresentialPurchaseResponse(
                purchaseId,
                studentId,
                buffetId,
                EstadoCompra.CANCELADO,
                "RET-123",
                new BigDecimal("1500.00")
        );
    }

    private Purchase purchase(UUID id, EstadoCompra status, int quantity) {
        Producto product = product();
        Purchase purchase = new Purchase();
        purchase.setId(id);
        purchase.setStatus(status);
        purchase.setBuffetId(buffetId);
        purchase.setDate(NOW);
        purchase.setTotalAmount(product.getPrecio().multiply(BigDecimal.valueOf(quantity)));
        purchase.setItems(Map.of(product, quantity));
        return purchase;
    }

    private Producto product() {
        Producto product = new Producto();
        product.setId(UUID.randomUUID());
        product.setNombre("Alfajor");
        product.setPrecio(new BigDecimal("500.00"));
        return product;
    }
}
