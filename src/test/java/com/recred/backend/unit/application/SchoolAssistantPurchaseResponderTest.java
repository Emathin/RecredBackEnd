package com.recred.backend.unit.application;

import com.recred.backend.application.CreateAssistantAdvancePurchase;
import com.recred.backend.application.SchoolAssistantContentFormatter;
import com.recred.backend.application.SchoolAssistantFixedReply;
import com.recred.backend.application.SchoolAssistantIntentDetector;
import com.recred.backend.application.SchoolAssistantPendingPurchaseParser;
import com.recred.backend.application.SchoolAssistantPendingPurchaseResponder;
import com.recred.backend.application.SchoolAssistantPurchaseFailureMessages;
import com.recred.backend.application.SchoolAssistantPurchasePatternService;
import com.recred.backend.application.SchoolAssistantPurchaseRequestParser;
import com.recred.backend.application.SchoolAssistantPurchaseResponder;
import com.recred.backend.application.SchoolAssistantQuickPurchaseHandler;
import com.recred.backend.application.SchoolAssistantQuickPurchaseReplyFactory;
import com.recred.backend.application.SchoolAssistantReplyBuilder;
import com.recred.backend.application.SchoolAssistantReplyFormatter;
import com.recred.backend.application.SchoolAssistantSuggestionFactory;
import com.recred.backend.domain.models.ChatMessage;
import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.MedioPago;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.RolChatMensaje;
import com.recred.backend.domain.models.SchoolAssistantActionStatus;
import com.recred.backend.domain.models.SchoolAssistantActionType;
import com.recred.backend.domain.models.SchoolAssistantContext;
import com.recred.backend.domain.models.SchoolAssistantIntent;
import com.recred.backend.domain.models.SchoolAssistantRole;
import com.recred.backend.domain.models.TipoPlan;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.ProductRepository;
import com.recred.backend.domain.repositories.PurchaseRepository;
import com.recred.backend.domain.repositories.TutorRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchoolAssistantPurchaseResponderTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 27, 12, 0);

    @Mock
    private PurchaseRepository purchaseRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private TutorRepository tutorRepository;

    @Mock
    private CreateAssistantAdvancePurchase createAssistantAdvancePurchase;

    private SchoolAssistantPurchaseResponder responder;
    private UUID studentId;
    private UUID sessionId;
    private UUID buffetId;
    private Producto product;
    private SchoolAssistantContext context;
    private List<ChatMessage> recentMessages;
    private Optional<SchoolAssistantFixedReply> reply;

    @BeforeEach
    void setUp() {
        responder = purchaseResponder();
        studentId = UUID.randomUUID();
        sessionId = UUID.randomUUID();
        buffetId = UUID.randomUUID();
        recentMessages = List.of();
        lenient().when(tutorRepository.findByAlumnoId(any()))
                .thenReturn(List.of(tutorWithPlan(TipoPlan.AVANZADO)));
    }

    private SchoolAssistantPurchaseResponder purchaseResponder() {
        Clock clock = fixedClock();
        SchoolAssistantIntentDetector intentDetector = new SchoolAssistantIntentDetector();
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
        SchoolAssistantPendingPurchaseParser pendingPurchaseParser =
                new SchoolAssistantPendingPurchaseParser(intentDetector);
        SchoolAssistantPendingPurchaseResponder pendingPurchaseResponder =
                new SchoolAssistantPendingPurchaseResponder(
                        productRepository,
                        createAssistantAdvancePurchase,
                        clock,
                        new SchoolAssistantPurchaseFailureMessages(),
                        pendingPurchaseParser,
                        replyFactory
                );
        SchoolAssistantPurchasePatternService purchasePatternService =
                new SchoolAssistantPurchasePatternService(
                        purchaseRepository,
                        productRepository,
                        contentFormatter
                );
        return new SchoolAssistantPurchaseResponder(
                new SchoolAssistantPurchaseRequestParser(intentDetector, clock),
                new SchoolAssistantQuickPurchaseHandler(
                        purchasePatternService,
                        tutorRepository,
                        clock,
                        replyFactory,
                        pendingPurchaseResponder
                ),
                replyBuilder
        );
    }

    private Clock fixedClock() {
        return Clock.fixed(
                Instant.parse("2026-05-27T12:00:00Z"),
                ZoneId.of("America/Argentina/Buenos_Aires")
        );
    }

    @Test
    void respond_WhenStudentRepeatsLastPurchase_ShouldCreatePendingPurchaseReply() {
        givenStudentContext();
        givenLastPurchaseWithActiveProduct();

        whenRespondingTo("repeti mi ultima compra");

        thenPendingPurchaseWasCreated();
    }

    @Test
    void respond_WhenTutorRequestsQuickPurchase_ShouldReturnForbiddenReply() {
        givenTutorContext();

        whenRespondingTo("comprame lo de siempre");

        thenForbiddenPurchaseReplyWasReturned();
        thenNoPurchaseDependenciesWereUsed();
    }

    @Test
    void respond_WhenStudentConfirmsWithoutPendingPurchase_ShouldExplainMissingDraft() {
        givenStudentContext();

        whenRespondingTo("confirmar");

        thenMissingPendingPurchaseReplyWasReturned();
    }

    @Test
    void respond_WhenStudentCancelsPurchaseWithPendingDraft_ShouldDiscardDraft() {
        givenStudentContext();
        givenPendingPurchaseDraft();

        whenRespondingTo("cancelar mi compra");

        thenPendingDraftWasDiscarded();
    }

    @Test
    void respond_WhenStudentCancelsPurchaseWithoutPendingDraft_ShouldReturnEmpty() {
        givenStudentContext();

        whenRespondingTo("cancelar mi compra");

        thenNoPurchaseReplyWasReturned();
    }

    @Test
    void respond_WhenMessageIsNotPurchase_ShouldReturnEmpty() {
        givenStudentContext();

        whenRespondingTo("cuanto saldo tengo");

        thenNoPurchaseReplyWasReturned();
    }

    private void givenStudentContext() {
        context = new SchoolAssistantContext(
                studentId,
                Optional.of(new BigDecimal("5000.00")),
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

    private void givenLastPurchaseWithActiveProduct() {
        product = product("Agua", new BigDecimal("700.00"));
        Purchase purchase = purchase(product, 2);
        when(purchaseRepository.findHistorialByAlumno(studentId)).thenReturn(List.of(purchase));
        when(productRepository.findActiveById(product.getId())).thenReturn(Optional.of(product));
    }

    private void givenPendingPurchaseDraft() {
        product = product("Yogur", new BigDecimal("900.00"));
        recentMessages = List.of(assistantMessage(pendingMetadata(product.getId(), 1)));
    }

    private void whenRespondingTo(String message) {
        reply = responder.respond(message, context, recentMessages);
    }

    private void thenPendingPurchaseWasCreated() {
        SchoolAssistantFixedReply answer = presentReply();
        assertTrue(answer.answer().contains("Puedo repetir tu ultima compra"));
        assertEquals(SchoolAssistantIntent.REPEAT_LAST_PURCHASE, answer.intent());
        assertEquals(SchoolAssistantActionType.COMPRA_ANTICIPADA, answer.action().type());
        assertEquals(SchoolAssistantActionStatus.ESPERANDO_FECHA, answer.action().status());
        assertEquals("PENDIENTE", answer.metadata().get("estadoAccionChatbot"));
        thenPendingActionReferencesProduct(answer);
    }

    private void thenPendingActionReferencesProduct(SchoolAssistantFixedReply answer) {
        Map<?, ?> action = (Map<?, ?>) answer.metadata().get("accionPendienteChatbot");
        List<?> items = (List<?>) action.get("items");
        Map<?, ?> item = (Map<?, ?>) items.get(0);
        assertEquals(studentId.toString(), action.get("alumnoId"));
        assertEquals(buffetId.toString(), action.get("buffetId"));
        assertEquals(product.getId().toString(), item.get("productoId"));
        assertEquals(2, item.get("cantidad"));
    }

    private void thenForbiddenPurchaseReplyWasReturned() {
        SchoolAssistantFixedReply answer = presentReply();
        assertEquals(
                "Por ahora solo los alumnos pueden hacer compras rapidas desde el asistente.",
                answer.answer()
        );
        assertEquals(SchoolAssistantIntent.BUY_USUAL, answer.intent());
        assertEquals(SchoolAssistantActionStatus.NO_PERMITIDA, answer.action().status());
    }

    private void thenNoPurchaseDependenciesWereUsed() {
        verifyNoInteractions(
                purchaseRepository,
                productRepository,
                tutorRepository,
                createAssistantAdvancePurchase
        );
    }

    private void thenMissingPendingPurchaseReplyWasReturned() {
        SchoolAssistantFixedReply answer = presentReply();
        assertEquals("No tengo ninguna compra pendiente para confirmar.", answer.answer());
        assertEquals(SchoolAssistantIntent.CONFIRM_PURCHASE, answer.intent());
        assertEquals(SchoolAssistantActionStatus.FALLIDA, answer.action().status());
    }

    private void thenPendingDraftWasDiscarded() {
        SchoolAssistantFixedReply answer = presentReply();
        assertEquals("Listo, descarte esa compra pendiente.", answer.answer());
        assertEquals(SchoolAssistantIntent.CANCEL_ACTION, answer.intent());
        assertEquals(SchoolAssistantActionStatus.CANCELADA, answer.action().status());
    }

    private void thenNoPurchaseReplyWasReturned() {
        assertFalse(reply.isPresent());
    }

    private SchoolAssistantFixedReply presentReply() {
        assertTrue(reply.isPresent());
        return reply.orElseThrow();
    }

    private Producto product(String name, BigDecimal price) {
        Producto product = new Producto();
        product.setId(UUID.randomUUID());
        product.setNombre(name);
        product.setPrecio(price);
        product.setRequierePreparacion(false);
        return product;
    }

    private Purchase purchase(Producto product, int quantity) {
        Purchase purchase = new Purchase();
        purchase.setTotalAmount(product.getPrecio().multiply(BigDecimal.valueOf(quantity)));
        purchase.setStatus(EstadoCompra.ENTREGADO);
        purchase.setPaymentMethod(MedioPago.CREDITOS);
        purchase.setDate(NOW.minusDays(1));
        purchase.setItems(Map.of(product, quantity));
        purchase.setBuffetId(buffetId);
        return purchase;
    }

    private Tutor tutorWithPlan(TipoPlan plan) {
        Usuario usuario = new Usuario();
        usuario.setId(UUID.randomUUID());
        usuario.setPlan(plan);
        Tutor tutor = new Tutor();
        tutor.setUsuario(usuario);
        return tutor;
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

    private Map<String, Object> pendingMetadata(UUID productId, int quantity) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("estadoAccionChatbot", "PENDIENTE");
        metadata.put("accionPendienteChatbot", pendingAction(productId, quantity));
        return metadata;
    }

    private Map<String, Object> pendingAction(UUID productId, int quantity) {
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("tipo", "COMPRA_RAPIDA");
        action.put("alumnoId", studentId.toString());
        action.put("buffetId", buffetId.toString());
        action.put("items", List.of(pendingItem(productId, quantity)));
        return action;
    }

    private Map<String, Object> pendingItem(UUID productId, int quantity) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("productoId", productId.toString());
        item.put("cantidad", quantity);
        return item;
    }
}
