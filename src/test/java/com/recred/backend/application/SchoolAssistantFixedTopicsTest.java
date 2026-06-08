package com.recred.backend.application;

import com.recred.backend.domain.models.ChatMessage;
import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.MedioPago;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.RecessTime;
import com.recred.backend.domain.models.RolChatMensaje;
import com.recred.backend.domain.models.SchoolAssistantActionStatus;
import com.recred.backend.domain.models.SchoolAssistantActionType;
import com.recred.backend.domain.models.SchoolAssistantContext;
import com.recred.backend.domain.models.SchoolAssistantIntent;
import com.recred.backend.domain.models.SchoolAssistantRequiredInput;
import com.recred.backend.domain.models.SchoolAssistantRole;
import com.recred.backend.domain.models.SchoolCalendarEvent;
import com.recred.backend.domain.models.TipoDiaEscolar;
import com.recred.backend.domain.repositories.ProductRepository;
import com.recred.backend.domain.repositories.PurchaseRepository;
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
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchoolAssistantFixedTopicsTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 27, 12, 0);

    @Mock
    private PurchaseRepository purchaseRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CreateAssistantAdvancePurchase createAssistantAdvancePurchase;

    private SchoolAssistantFixedTopics fixedTopics;
    private UUID studentId;
    private UUID sessionId;
    private UUID buffetId;

    @BeforeEach
    void setUp() {
        fixedTopics = new SchoolAssistantFixedTopics(
                purchaseRepository,
                productRepository,
                createAssistantAdvancePurchase,
                Clock.fixed(Instant.parse("2026-05-27T12:00:00Z"),
                        ZoneId.of("America/Argentina/Buenos_Aires"))
        );
        studentId = UUID.randomUUID();
        sessionId = UUID.randomUUID();
        buffetId = UUID.randomUUID();
    }

    @Test
    void respondsBalanceTopic() {
        Optional<SchoolAssistantFixedReply> reply = fixedTopics.respond(
                "cuanto saldo tengo",
                studentContext(new BigDecimal("500.00")),
                List.of()
        );

        assertTrue(reply.isPresent());
        assertEquals("Tenes $500.00 disponibles.", reply.orElseThrow().answer());
        assertEquals("BALANCE", reply.orElseThrow().metadata().get("intencionChatbot"));
        assertEquals(SchoolAssistantIntent.BALANCE, reply.orElseThrow().intent());
        assertEquals(SchoolAssistantActionType.CONSULTA_SALDO,
                reply.orElseThrow().action().type());
        assertEquals(SchoolAssistantActionStatus.INFORMATIVA,
                reply.orElseThrow().action().status());
        verifyNoInteractions(purchaseRepository, productRepository, createAssistantAdvancePurchase);
    }

    @Test
    void respondsRecentPurchasesTopic() {
        Producto product = product("Sandwich", new BigDecimal("1200.00"));
        Purchase purchase = purchase(product, 1, NOW.minusHours(2));

        when(purchaseRepository.findHistorialByAlumno(studentId)).thenReturn(List.of(purchase));

        Optional<SchoolAssistantFixedReply> reply = fixedTopics.respond(
                "mostrame mis ultimas compras",
                studentContext(new BigDecimal("5000.00")),
                List.of()
        );

        assertTrue(reply.isPresent());
        assertTrue(reply.orElseThrow().answer().contains("1x Sandwich"));
        assertTrue(reply.orElseThrow().answer().contains("$1200.00"));
        assertTrue(reply.orElseThrow().answer().contains("ENTREGADO"));
    }

    @Test
    void createsPendingActionForLastPurchase() {
        Producto product = product("Agua", new BigDecimal("700.00"));
        Purchase purchase = purchase(product, 2, NOW.minusDays(1));

        when(purchaseRepository.findHistorialByAlumno(studentId)).thenReturn(List.of(purchase));
        when(productRepository.findActiveById(product.getId())).thenReturn(Optional.of(product));

        Optional<SchoolAssistantFixedReply> reply = fixedTopics.respond(
                "repeti mi ultima compra",
                studentContext(new BigDecimal("5000.00")),
                List.of()
        );

        assertTrue(reply.isPresent());
        assertTrue(reply.orElseThrow().answer().contains("2x Agua"));
        assertTrue(reply.orElseThrow().answer().contains("Elegi el recreo"));
        assertEquals("PENDIENTE", reply.orElseThrow().metadata().get("estadoAccionChatbot"));
        assertEquals(SchoolAssistantActionType.COMPRA_ANTICIPADA,
                reply.orElseThrow().action().type());
        assertEquals(SchoolAssistantActionStatus.ESPERANDO_RECREO,
                reply.orElseThrow().action().status());
        assertEquals(List.of(SchoolAssistantRequiredInput.RECREO),
                reply.orElseThrow().action().requiredInputs());

        Map<?, ?> action = (Map<?, ?>) reply.orElseThrow()
                .metadata()
                .get("accionPendienteChatbot");
        assertEquals(buffetId.toString(), action.get("buffetId"));
        List<?> items = (List<?>) action.get("items");
        Map<?, ?> item = (Map<?, ?>) items.get(0);
        assertEquals(product.getId().toString(), item.get("productoId"));
        assertEquals(2, item.get("cantidad"));
    }

    @Test
    void confirmationAsksRecessWhenPendingPurchaseHasNoRecess() {
        Producto product = product("Yogur", new BigDecimal("900.00"));

        Optional<SchoolAssistantFixedReply> reply = fixedTopics.respond(
                "confirmar",
                studentContext(new BigDecimal("5000.00")),
                List.of(assistantMessage(pendingMetadata(product.getId(), 1)))
        );

        assertTrue(reply.isPresent());
        assertTrue(reply.orElseThrow().answer().contains("Elegi el recreo"));
        assertEquals("PENDIENTE", reply.orElseThrow().metadata().get("estadoAccionChatbot"));
        verifyNoInteractions(createAssistantAdvancePurchase);
    }

    @Test
    void selectsRecessForPendingPurchase() {
        Producto product = product("Yogur", new BigDecimal("900.00"));

        Optional<SchoolAssistantFixedReply> reply = fixedTopics.respond(
                "primer recreo",
                studentContext(new BigDecimal("5000.00")),
                List.of(assistantMessage(pendingMetadata(product.getId(), 1)))
        );

        assertTrue(reply.isPresent());
        assertTrue(reply.orElseThrow().answer().contains("primer recreo"));
        assertEquals(SchoolAssistantActionStatus.ESPERANDO_CONFIRMACION,
                reply.orElseThrow().action().status());
        assertEquals(RecessTime.FIRST_RECESS, reply.orElseThrow().action().recessTime());
        Map<?, ?> action = (Map<?, ?>) reply.orElseThrow()
                .metadata()
                .get("accionPendienteChatbot");
        assertEquals("FIRST_RECESS", action.get("recreo"));
    }

    @Test
    void confirmsPendingAdvancePurchaseWithRecess() {
        Producto product = product("Yogur", new BigDecimal("900.00"));
        Purchase savedPurchase = purchase(product, 1, NOW);
        savedPurchase.setId(UUID.randomUUID());
        savedPurchase.setStatus(EstadoCompra.PENDIENTE);
        savedPurchase.setWithdrawalCode("ABC123");

        when(productRepository.findActiveById(product.getId())).thenReturn(Optional.of(product));
        when(createAssistantAdvancePurchase.execute(
                eq(studentId),
                eq(buffetId),
                argThat(cart -> cart.containsValue(1)),
                eq(RecessTime.FIRST_RECESS),
                eq(LocalDate.of(2026, 5, 27))
        )).thenReturn(savedPurchase);

        Optional<SchoolAssistantFixedReply> reply = fixedTopics.respond(
                "confirmar",
                studentContext(new BigDecimal("5000.00")),
                List.of(assistantMessage(pendingMetadata(
                        product.getId(),
                        1,
                        RecessTime.FIRST_RECESS
                )))
        );

        assertTrue(reply.isPresent());
        assertTrue(reply.orElseThrow().answer().contains("Compra anticipada creada"));
        assertTrue(reply.orElseThrow().answer().contains("ABC123"));
        assertEquals("EJECUTADA", reply.orElseThrow().metadata().get("estadoAccionChatbot"));
        assertEquals(SchoolAssistantActionStatus.EJECUTADA,
                reply.orElseThrow().action().status());
        assertEquals(savedPurchase.getId(), reply.orElseThrow().action().purchaseId());
        assertEquals(EstadoCompra.PENDIENTE, reply.orElseThrow().action().purchaseStatus());
        assertEquals("ABC123", reply.orElseThrow().action().withdrawalCode());
        assertEquals(RecessTime.FIRST_RECESS, reply.orElseThrow().action().recessTime());
        assertEquals(savedPurchase.getId().toString(), reply.orElseThrow().metadata().get(
                "compraId"
        ));
        verify(createAssistantAdvancePurchase).execute(
                eq(studentId),
                eq(buffetId),
                argThat(cart -> cart.containsKey(product) && cart.containsValue(1)),
                eq(RecessTime.FIRST_RECESS),
                eq(LocalDate.of(2026, 5, 27))
        );
    }

    @Test
    void createsPendingActionForUsualPurchasePattern() {
        Producto product = product("Barrita", new BigDecimal("650.00"));
        Purchase firstPurchase = purchase(product, 1, NOW.minusDays(1));
        Purchase secondPurchase = purchase(product, 1, NOW.minusDays(3));

        when(purchaseRepository.findHistorialByAlumno(studentId)).thenReturn(List.of(
                firstPurchase,
                secondPurchase
        ));
        when(productRepository.findActiveById(product.getId())).thenReturn(Optional.of(product));

        Optional<SchoolAssistantFixedReply> reply = fixedTopics.respond(
                "comprame lo de siempre",
                studentContext(new BigDecimal("5000.00")),
                List.of()
        );

        assertTrue(reply.isPresent());
        assertTrue(reply.orElseThrow().answer().contains("Tu compra habitual detectada"));
        assertTrue(reply.orElseThrow().answer().contains("1x Barrita"));
        assertEquals("PENDIENTE", reply.orElseThrow().metadata().get("estadoAccionChatbot"));
    }

    @Test
    void respondsEventsTopicForStudent() {
        SchoolCalendarEvent event = new SchoolCalendarEvent(
                LocalDate.of(2026, 6, 1),
                TipoDiaEscolar.JORNADA_ESPECIAL,
                "Acto escolar"
        );

        Optional<SchoolAssistantFixedReply> reply = fixedTopics.respond(
                "que eventos tengo",
                studentContext(new BigDecimal("5000.00"), List.of(event)),
                List.of()
        );

        assertTrue(reply.isPresent());
        assertTrue(reply.orElseThrow().answer().contains("Acto escolar"));
        assertEquals(SchoolAssistantIntent.EVENTS, reply.orElseThrow().intent());
        assertEquals(SchoolAssistantActionType.CONSULTA_EVENTOS,
                reply.orElseThrow().action().type());
    }

    @Test
    void tutorEventsDoNotSuggestStudentQuickPurchase() {
        SchoolCalendarEvent event = new SchoolCalendarEvent(
                LocalDate.of(2026, 6, 1),
                TipoDiaEscolar.JORNADA_ESPECIAL,
                "Acto escolar"
        );

        Optional<SchoolAssistantFixedReply> reply = fixedTopics.respond(
                "que eventos tienen mis hijos",
                tutorContext(List.of(event)),
                List.of()
        );

        assertTrue(reply.isPresent());
        SchoolAssistantFixedReply answer = reply.orElseThrow();
        assertEquals(SchoolAssistantIntent.EVENTS, answer.intent());
        assertTrue(answer.suggestions().stream()
                .noneMatch(suggestion -> suggestion.actionType()
                        == SchoolAssistantActionType.COMPRA_ANTICIPADA));
        assertTrue(answer.suggestions().stream()
                .anyMatch(suggestion -> suggestion.actionType()
                        == SchoolAssistantActionType.CONSULTA_HIJOS));
    }

    @Test
    void respondsStockTopicForKiosquero() {
        Producto product = product("Sandwich", new BigDecimal("1200.00"));
        product.setStockActual(4);
        when(productRepository.findAllActiveWithStock(buffetId)).thenReturn(List.of(product));

        Optional<SchoolAssistantFixedReply> reply = fixedTopics.respond(
                "ver stock",
                kiosqueroContext(),
                List.of()
        );

        assertTrue(reply.isPresent());
        assertTrue(reply.orElseThrow().answer().contains("Sandwich"));
        assertEquals(SchoolAssistantIntent.STOCK, reply.orElseThrow().intent());
        assertEquals(SchoolAssistantActionType.CONSULTA_STOCK,
                reply.orElseThrow().action().type());
    }

    @Test
    void kiosqueroQuickPurchaseForbiddenDoesNotSuggestStudentQuickPurchase() {
        Optional<SchoolAssistantFixedReply> reply = fixedTopics.respond(
                "comprame lo de siempre",
                kiosqueroContext(),
                List.of()
        );

        assertTrue(reply.isPresent());
        SchoolAssistantFixedReply answer = reply.orElseThrow();
        assertEquals(SchoolAssistantActionStatus.NO_PERMITIDA, answer.action().status());
        assertTrue(answer.suggestions().stream()
                .noneMatch(suggestion -> suggestion.actionType()
                        == SchoolAssistantActionType.COMPRA_ANTICIPADA));
        assertTrue(answer.suggestions().stream()
                .anyMatch(suggestion -> suggestion.actionType()
                        == SchoolAssistantActionType.CONSULTA_PEDIDOS));
    }

    @Test
    void tutorBudgetQuestionWinsOverChildrenKeyword() {
        Optional<SchoolAssistantFixedReply> reply = fixedTopics.respond(
                "ver presupuestos de mis hijos",
                tutorContext(),
                List.of()
        );

        assertTrue(reply.isPresent());
        SchoolAssistantFixedReply answer = reply.orElseThrow();
        assertEquals(SchoolAssistantIntent.BUDGETS, answer.intent());
        assertEquals(SchoolAssistantActionType.CONSULTA_PRESUPUESTOS,
                answer.action().type());
        assertTrue(answer.suggestions().stream()
                .noneMatch(suggestion -> suggestion.actionType()
                        == SchoolAssistantActionType.COMPRA_ANTICIPADA));
    }

    @Test
    void tutorRestrictionsQuestionWinsOverChildrenKeyword() {
        Optional<SchoolAssistantFixedReply> reply = fixedTopics.respond(
                "ver restricciones de mis hijos",
                tutorContext(),
                List.of()
        );

        assertTrue(reply.isPresent());
        SchoolAssistantFixedReply answer = reply.orElseThrow();
        assertEquals(SchoolAssistantIntent.RESTRICTIONS, answer.intent());
        assertEquals(SchoolAssistantActionType.CONSULTA_RESTRICCIONES,
                answer.action().type());
        assertTrue(answer.suggestions().stream()
                .noneMatch(suggestion -> suggestion.actionType()
                        == SchoolAssistantActionType.COMPRA_ANTICIPADA));
    }

    @Test
    void respondsChildrenTopicForTutor() {
        Optional<SchoolAssistantFixedReply> reply = fixedTopics.respond(
                "mostrame mis hijos",
                tutorContext(),
                List.of()
        );

        assertTrue(reply.isPresent());
        assertTrue(reply.orElseThrow().answer().contains("Mateo Perez"));
        assertEquals(SchoolAssistantIntent.CHILDREN, reply.orElseThrow().intent());
        assertEquals(SchoolAssistantActionType.CONSULTA_HIJOS,
                reply.orElseThrow().action().type());
    }

    private SchoolAssistantContext studentContext(BigDecimal balance) {
        return studentContext(balance, List.of());
    }

    private SchoolAssistantContext studentContext(
            BigDecimal balance,
            List<SchoolCalendarEvent> events
    ) {
        return new SchoolAssistantContext(
                studentId,
                Optional.ofNullable(balance),
                List.of(),
                List.of(),
                events
        );
    }

    private SchoolAssistantContext kiosqueroContext() {
        return new SchoolAssistantContext(
                null,
                Optional.empty(),
                List.of(),
                List.of(),
                List.of(),
                SchoolAssistantRole.KIOSQUERO,
                UUID.randomUUID(),
                buffetId,
                List.of(),
                List.of()
        );
    }

    private SchoolAssistantContext tutorContext() {
        return tutorContext(List.of());
    }

    private SchoolAssistantContext tutorContext(List<SchoolCalendarEvent> events) {
        return new SchoolAssistantContext(
                null,
                Optional.empty(),
                List.of(),
                List.of(),
                events,
                SchoolAssistantRole.TUTOR,
                UUID.randomUUID(),
                null,
                List.of("hijo=Mateo Perez|saldo=900.00|presupuesto=2000"),
                List.of()
        );
    }

    private Producto product(String name, BigDecimal price) {
        Producto product = new Producto();
        product.setId(UUID.randomUUID());
        product.setNombre(name);
        product.setPrecio(price);
        product.setRequierePreparacion(false);
        return product;
    }

    private Purchase purchase(Producto product, int quantity, LocalDateTime dateTime) {
        Purchase purchase = new Purchase();
        purchase.setStudent(null);
        purchase.setTotalAmount(product.getPrecio().multiply(BigDecimal.valueOf(quantity)));
        purchase.setStatus(EstadoCompra.ENTREGADO);
        purchase.setPaymentMethod(MedioPago.CREDITOS);
        purchase.setDate(dateTime);
        purchase.setItems(Map.of(product, quantity));
        purchase.setBuffetId(buffetId);
        return purchase;
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
        return pendingMetadata(productId, quantity, null);
    }

    private Map<String, Object> pendingMetadata(
            UUID productId,
            int quantity,
            RecessTime recessTime
    ) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("estadoAccionChatbot", "PENDIENTE");
        metadata.put("accionPendienteChatbot", pendingAction(productId, quantity, recessTime));
        return metadata;
    }

    private Map<String, Object> pendingAction(
            UUID productId,
            int quantity,
            RecessTime recessTime
    ) {
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("tipo", "COMPRA_RAPIDA");
        action.put("alumnoId", studentId.toString());
        action.put("buffetId", buffetId.toString());
        if (recessTime != null)
            action.put("recreo", recessTime.name());
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
