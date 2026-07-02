package com.recred.backend.unit.application;

import com.recred.backend.application.CancelPresentialPurchase;
import com.recred.backend.application.CancelPresentialPurchaseResponse;
import com.recred.backend.application.CreateAssistantAdvancePurchase;
import com.recred.backend.application.CreateAssistantAdvancePurchaseRequest;
import com.recred.backend.application.CreateAssistantAdvancePurchaseResponse;
import com.recred.backend.application.SchoolAssistantFixedReply;
import com.recred.backend.application.SchoolAssistantFixedTopics;
import com.recred.backend.domain.exceptions.InsufficientBalanceException;
import com.recred.backend.domain.models.Alumno;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    @Mock
    private CancelPresentialPurchase cancelPresentialPurchase;

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
                cancelPresentialPurchase,
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
        assertTrue(reply.orElseThrow().answer().contains("Entregado"));
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
        assertTrue(reply.orElseThrow().answer()
                .contains("Puedo repetir tu ultima compra con estos productos"));
        assertTrue(reply.orElseThrow().answer().contains("\n- 2x Agua"));
        assertTrue(reply.orElseThrow().answer().contains("Total estimado: $1400.00"));
        assertTrue(reply.orElseThrow().answer()
                .contains("Para terminar, elegi el dia de retiro."));
        assertTrue(reply.orElseThrow().suggestions().isEmpty());
        assertEquals("PENDIENTE", reply.orElseThrow().metadata().get("estadoAccionChatbot"));
        assertEquals(SchoolAssistantActionType.COMPRA_ANTICIPADA,
                reply.orElseThrow().action().type());
        assertEquals(SchoolAssistantActionStatus.ESPERANDO_FECHA,
                reply.orElseThrow().action().status());
        assertEquals(List.of(SchoolAssistantRequiredInput.FECHA_RETIRO),
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
    void confirmationAsksDateWhenPendingPurchaseHasNoDate() {
        Producto product = product("Yogur", new BigDecimal("900.00"));

        Optional<SchoolAssistantFixedReply> reply = fixedTopics.respond(
                "confirmar",
                studentContext(new BigDecimal("5000.00")),
                List.of(assistantMessage(pendingMetadata(product.getId(), 1)))
        );

        assertTrue(reply.isPresent());
        assertTrue(reply.orElseThrow().answer()
                .contains("Para terminar, elegi el dia de retiro."));
        assertTrue(reply.orElseThrow().suggestions().isEmpty());
        assertEquals("PENDIENTE", reply.orElseThrow().metadata().get("estadoAccionChatbot"));
        verifyNoInteractions(createAssistantAdvancePurchase);
    }

    @Test
    void selectsPurchaseDateForPendingPurchase() {
        Producto product = product("Yogur", new BigDecimal("900.00"));

        Optional<SchoolAssistantFixedReply> reply = fixedTopics.respond(
                "mañana",
                studentContext(new BigDecimal("5000.00")),
                List.of(assistantMessage(pendingMetadata(product.getId(), 1)))
        );

        assertTrue(reply.isPresent());
        assertTrue(reply.orElseThrow().answer().contains("28/5/2026"));
        assertEquals(SchoolAssistantActionStatus.ESPERANDO_RECREO,
                reply.orElseThrow().action().status());
        Map<?, ?> action = (Map<?, ?>) reply.orElseThrow()
                .metadata()
                .get("accionPendienteChatbot");
        assertEquals("2026-05-28", action.get("fechaRetiro"));
    }

    @Test
    void selectPastPurchaseDateForPendingPurchaseFails() {
        Producto product = product("Yogur", new BigDecimal("900.00"));

        Optional<SchoolAssistantFixedReply> reply = fixedTopics.respond(
                "1/5/2026",
                studentContext(new BigDecimal("5000.00")),
                List.of(assistantMessage(pendingMetadata(product.getId(), 1)))
        );

        assertTrue(reply.isPresent());
        assertEquals("No puedo programar una compra para una fecha pasada.",
                reply.orElseThrow().answer());
        assertEquals(SchoolAssistantActionStatus.FALLIDA,
                reply.orElseThrow().action().status());
        verifyNoInteractions(createAssistantAdvancePurchase);
    }

    @Test
    void selectsRecessForPendingPurchaseWithDate() {
        Producto product = product("Yogur", new BigDecimal("900.00"));

        Optional<SchoolAssistantFixedReply> reply = fixedTopics.respond(
                "primer recreo",
                studentContext(new BigDecimal("5000.00")),
                List.of(assistantMessage(pendingMetadata(
                        product.getId(),
                        1,
                        LocalDate.of(2026, 5, 28),
                        null
                )))
        );

        assertTrue(reply.isPresent());
        assertTrue(reply.orElseThrow().answer().contains("28/5/2026 en primer recreo"));
        assertEquals(SchoolAssistantActionStatus.ESPERANDO_CONFIRMACION,
                reply.orElseThrow().action().status());
        assertEquals(RecessTime.FIRST_RECESS, reply.orElseThrow().action().recessTime());
        Map<?, ?> action = (Map<?, ?>) reply.orElseThrow()
                .metadata()
                .get("accionPendienteChatbot");
        assertEquals("2026-05-28", action.get("fechaRetiro"));
        assertEquals("FIRST_RECESS", action.get("recreo"));
    }

    @Test
    void confirmsPendingAdvancePurchaseWithRecess() {
        Producto product = product("Yogur", new BigDecimal("900.00"));
        Purchase savedPurchase = purchase(product, 1, NOW);
        savedPurchase.setStudent(alumno(studentId));
        savedPurchase.setId(UUID.randomUUID());
        savedPurchase.setStatus(EstadoCompra.PENDIENTE);
        savedPurchase.setWithdrawalCode("ABC123");

        when(productRepository.findActiveById(product.getId())).thenReturn(Optional.of(product));
        when(createAssistantAdvancePurchase.execute(argThat(request -> matchesPurchaseRequest(
                request,
                product,
                1,
                RecessTime.FIRST_RECESS,
                LocalDate.of(2026, 5, 28)
        )))).thenReturn(responseFrom(savedPurchase));

        Optional<SchoolAssistantFixedReply> reply = fixedTopics.respond(
                "confirmar",
                studentContext(new BigDecimal("5000.00")),
                List.of(assistantMessage(pendingMetadata(
                        product.getId(),
                        1,
                        LocalDate.of(2026, 5, 28),
                        RecessTime.FIRST_RECESS
                )))
        );

        assertTrue(reply.isPresent());
        SchoolAssistantFixedReply answer = reply.orElseThrow();
        assertTrue(answer.answer().contains("Listo, tu compra anticipada quedo creada"));
        assertTrue(answer.answer().contains("Fecha de retiro: 28/5/2026"));
        assertTrue(answer.answer().contains("ABC123"));
        assertEquals("EJECUTADA", answer.metadata().get("estadoAccionChatbot"));
        assertEquals(SchoolAssistantActionStatus.EJECUTADA,
                answer.action().status());
        assertEquals(savedPurchase.getId(), answer.action().purchaseId());
        assertEquals(EstadoCompra.PENDIENTE, answer.action().purchaseStatus());
        assertEquals("ABC123", answer.action().withdrawalCode());
        assertEquals(RecessTime.FIRST_RECESS, answer.action().recessTime());
        assertEquals(savedPurchase.getId().toString(), answer.metadata().get(
                "compraId"
        ));
        assertEquals(List.of("Cancelar pedido", "Codigo retiro", "Compra habitual"),
                answer.suggestions().stream()
                        .map(suggestion -> suggestion.label())
                        .toList());
        assertTrue(answer.suggestions().stream()
                .anyMatch(suggestion -> suggestion.message().equals("cancelar mi pedido")
                        && suggestion.actionType()
                                == SchoolAssistantActionType.CANCELACION_COMPRA));
        verify(createAssistantAdvancePurchase).execute(argThat(request -> matchesPurchaseRequest(
                request,
                product,
                1,
                RecessTime.FIRST_RECESS,
                LocalDate.of(2026, 5, 28)
        )));
    }

    @Test
    void confirmsPendingAdvancePurchaseWithInsufficientBalanceShowsFriendlyMessage() {
        Producto product = product("Yogur", new BigDecimal("900.00"));
        String technicalMessage = "Insufficient balance for student: " + studentId;

        when(productRepository.findActiveById(product.getId())).thenReturn(Optional.of(product));
        when(createAssistantAdvancePurchase.execute(argThat(request -> matchesPurchaseRequest(
                request,
                product,
                1,
                RecessTime.FIRST_RECESS,
                LocalDate.of(2026, 5, 28)
        )))).thenThrow(new InsufficientBalanceException(technicalMessage));

        Optional<SchoolAssistantFixedReply> reply = fixedTopics.respond(
                "confirmar",
                studentContext(new BigDecimal("5000.00")),
                List.of(assistantMessage(pendingMetadata(
                        product.getId(),
                        1,
                        LocalDate.of(2026, 5, 28),
                        RecessTime.FIRST_RECESS
                )))
        );

        assertTrue(reply.isPresent());
        SchoolAssistantFixedReply answer = reply.orElseThrow();
        assertEquals(
                "No pude realizar la compra porque no tenes saldo suficiente.",
                answer.answer()
        );
        assertFalse(answer.answer().contains(studentId.toString()));
        assertFalse(answer.answer().contains("Insufficient balance"));
        assertEquals("FALLIDA", answer.metadata().get("estadoAccionChatbot"));
        assertEquals(SchoolAssistantActionStatus.FALLIDA, answer.action().status());
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
        assertTrue(reply.orElseThrow().answer()
                .contains("Detecte esta compra como habitual"));
        assertTrue(reply.orElseThrow().answer().contains("1x Barrita"));
        assertEquals("PENDIENTE", reply.orElseThrow().metadata().get("estadoAccionChatbot"));
    }

    @Test
    void usualPurchaseWithPastDateFails() {
        Producto product = product("Barrita", new BigDecimal("650.00"));
        Purchase firstPurchase = purchase(product, 1, NOW.minusDays(1));
        Purchase secondPurchase = purchase(product, 1, NOW.minusDays(3));

        when(purchaseRepository.findHistorialByAlumno(studentId)).thenReturn(List.of(
                firstPurchase,
                secondPurchase
        ));
        when(productRepository.findActiveById(product.getId())).thenReturn(Optional.of(product));

        Optional<SchoolAssistantFixedReply> reply = fixedTopics.respond(
                "comprame lo de siempre 1/5/2026",
                studentContext(new BigDecimal("5000.00")),
                List.of()
        );

        assertTrue(reply.isPresent());
        assertEquals("No puedo programar una compra para una fecha pasada.",
                reply.orElseThrow().answer());
        assertEquals(SchoolAssistantIntent.BUY_USUAL, reply.orElseThrow().intent());
        assertEquals(SchoolAssistantActionStatus.FALLIDA,
                reply.orElseThrow().action().status());
        verifyNoInteractions(createAssistantAdvancePurchase);
    }

    @Test
    void frequentPurchasesWithoutPatternsUsesSecondPerson() {
        when(purchaseRepository.findHistorialByAlumno(studentId)).thenReturn(List.of());

        Optional<SchoolAssistantFixedReply> reply = fixedTopics.respond(
                "que compro seguido",
                studentContext(new BigDecimal("5000.00")),
                List.of()
        );

        assertTrue(reply.isPresent());
        assertEquals(
                "Todavia no tengo suficientes compras tuyas para detectar compras frecuentes.",
                reply.orElseThrow().answer()
        );
        assertFalse(reply.orElseThrow().answer().contains("este alumno"));
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
        assertTrue(reply.orElseThrow().answer().contains("1/6/2026 - Acto escolar"));
        assertFalse(reply.orElseThrow().answer().contains("2026-06-01 - Acto escolar"));
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
    void formatsLongStockAnswerAsReadablePreview() {
        List<Producto> products = List.of(
                product("Producto 1", new BigDecimal("100.00"), 10),
                product("Producto 2", new BigDecimal("200.00"), 20),
                product("Producto 3", new BigDecimal("300.00"), 30),
                product("Producto 4", new BigDecimal("400.00"), 40),
                product("Producto 5", new BigDecimal("500.00"), 50),
                product("Producto 6", new BigDecimal("600.00"), 60),
                product("Producto 7", new BigDecimal("700.00"), 70),
                product("Producto 8", new BigDecimal("800.00"), 80),
                product("Producto 9", new BigDecimal("900.00"), 90)
        );
        when(productRepository.findAllActiveWithStock(buffetId)).thenReturn(products);

        Optional<SchoolAssistantFixedReply> reply = fixedTopics.respond(
                "ver stock",
                kiosqueroContext(),
                List.of()
        );

        assertTrue(reply.isPresent());
        SchoolAssistantFixedReply answer = reply.orElseThrow();
        assertTrue(answer.answer().contains("Stock actual (9 productos):\n- Producto 1"));
        assertTrue(answer.answer().contains("\n- Producto 8"));
        assertTrue(answer.answer().contains("Y 1 mas en el detalle."));
        assertFalse(answer.answer().contains("Producto 9 por"));
        List<?> dataProducts = (List<?>) answer.action().data().get("productos");
        assertEquals(9, dataProducts.size());
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
        assertTrue(answer.answer().contains(
                "Presupuestos:\n- Mateo Perez: Limite mensual: $2000.00. "
                        + "Desde: 2026-06-07. Saldo: $900.00."
        ));
        assertFalse(answer.answer().contains("|id="));
        assertFalse(answer.answer().contains("presupuesto=monto"));
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
        assertTrue(answer.answer().contains(
                "Restricciones:\n- Mateo Perez: Salud: Sin TACC. "
                        + "Productos bloqueados: Gaseosa."
        ));
        assertFalse(answer.answer().contains("|productosBloqueados="));
        assertFalse(answer.answer().contains("restriccionesSalud="));
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
        SchoolAssistantFixedReply answer = reply.orElseThrow();
        assertTrue(answer.answer().contains(
                "Hijos asociados:\n- Mateo Perez - 2do B. Saldo: $900.00. "
                        + "Presupuesto: Limite mensual: $2000.00. Desde: 2026-06-07."
        ));
        assertFalse(answer.answer().contains("|saldo="));
        assertEquals(SchoolAssistantIntent.CHILDREN, answer.intent());
        assertEquals(SchoolAssistantActionType.CONSULTA_HIJOS,
                answer.action().type());
    }

    @Test
    void balanceWithoutRegisteredAmountUsesFallbackAnswer() {
        Optional<SchoolAssistantFixedReply> reply = fixedTopics.respond(
                "saldo disponible",
                studentContext(null),
                List.of()
        );

        assertTrue(reply.isPresent());
        assertEquals(
                "Todavia no veo saldo registrado en tu cuenta.",
                reply.orElseThrow().answer()
        );
        assertTrue(reply.orElseThrow().action().data().containsKey("saldo"));
    }

    @Test
    void recentPurchasesWithoutHistoryUsesEmptyAnswer() {
        when(purchaseRepository.findHistorialByAlumno(studentId)).thenReturn(List.of());

        Optional<SchoolAssistantFixedReply> reply = fixedTopics.respond(
                "historial de compras",
                studentContext(new BigDecimal("5000.00")),
                List.of()
        );

        assertTrue(reply.isPresent());
        assertEquals(
                "Todavia no veo compras registradas en tu historial.",
                reply.orElseThrow().answer()
        );
    }

    @Test
    void menuWithoutPreviousBuffetUsesMissingBuffetAnswer() {
        when(purchaseRepository.findHistorialByAlumno(studentId)).thenReturn(List.of());

        Optional<SchoolAssistantFixedReply> reply = fixedTopics.respond(
                "que venden",
                studentContext(new BigDecimal("5000.00")),
                List.of()
        );

        assertTrue(reply.isPresent());
        assertEquals(
                "Todavia no se que buffet usar para mostrarte productos.",
                reply.orElseThrow().answer()
        );
        assertEquals(SchoolAssistantActionType.CONSULTA_MENU,
                reply.orElseThrow().action().type());
    }

    @Test
    void menuWithPreviousBuffetShowsOnlyAvailableProducts() {
        Producto available = product("Ensalada", new BigDecimal("1500.00"));
        available.setStockActual(3);
        Producto unavailable = product("Tarta", new BigDecimal("1200.00"));
        unavailable.setStockActual(0);
        Purchase purchase = purchase(available, 1, NOW.minusDays(1));
        when(purchaseRepository.findHistorialByAlumno(studentId)).thenReturn(List.of(purchase));
        when(productRepository.findAllActiveWithStock(buffetId)).thenReturn(List.of(
                available,
                unavailable
        ));

        Optional<SchoolAssistantFixedReply> reply = fixedTopics.respond(
                "menu",
                studentContext(new BigDecimal("5000.00")),
                List.of()
        );

        assertTrue(reply.isPresent());
        assertTrue(reply.orElseThrow().answer().contains("Ensalada"));
        assertFalse(reply.orElseThrow().answer().contains("Tarta"));
        assertEquals(buffetId.toString(), reply.orElseThrow().action().data().get("buffetId"));
    }

    @Test
    void pendingOrdersForStudentShowsPendingPurchasesOnly() {
        Producto product = product("Medialuna", new BigDecimal("500.00"));
        Purchase pending = purchase(product, 2, NOW.minusMinutes(20));
        pending.setStatus(EstadoCompra.LISTO);
        Purchase delivered = purchase(product, 1, NOW.minusDays(1));
        when(purchaseRepository.findHistorialByAlumno(studentId)).thenReturn(List.of(
                pending,
                delivered
        ));

        Optional<SchoolAssistantFixedReply> reply = fixedTopics.respond(
                "pedidos pendientes",
                studentContext(new BigDecimal("5000.00")),
                List.of()
        );

        assertTrue(reply.isPresent());
        assertTrue(reply.orElseThrow().answer().contains("Medialuna"));
        assertTrue(reply.orElseThrow().answer().contains("Listo"));
        assertEquals(SchoolAssistantActionType.CONSULTA_PEDIDOS,
                reply.orElseThrow().action().type());
    }

    @Test
    void withdrawalCodeForStudentShowsFirstPendingCode() {
        Producto product = product("Jugo", new BigDecimal("800.00"));
        Purchase purchase = purchase(product, 1, NOW.minusMinutes(15));
        purchase.setStatus(EstadoCompra.LISTO);
        purchase.setWithdrawalCode("RET123");
        when(purchaseRepository.findHistorialByAlumno(studentId)).thenReturn(List.of(purchase));

        Optional<SchoolAssistantFixedReply> reply = fixedTopics.respond(
                "codigo de retiro",
                studentContext(new BigDecimal("5000.00")),
                List.of()
        );

        assertTrue(reply.isPresent());
        assertEquals("Tu codigo de retiro es RET123.", reply.orElseThrow().answer());
        assertEquals(SchoolAssistantActionType.CONSULTA_CODIGO_RETIRO,
                reply.orElseThrow().action().type());
    }

    @Test
    void cancellationWithPendingActionCancelsIt() {
        Producto product = product("Yogur", new BigDecimal("900.00"));

        Optional<SchoolAssistantFixedReply> reply = fixedTopics.respond(
                "cancelar",
                studentContext(new BigDecimal("5000.00")),
                List.of(assistantMessage(pendingMetadata(product.getId(), 1)))
        );

        assertTrue(reply.isPresent());
        assertEquals("Listo, descarte esa compra pendiente.", reply.orElseThrow().answer());
        assertEquals("CANCELADA", reply.orElseThrow().metadata().get("estadoAccionChatbot"));
        assertEquals(SchoolAssistantActionStatus.CANCELADA,
                reply.orElseThrow().action().status());
    }

    @Test
    void purchaseCancellationMessageWithPendingActionCancelsDraftFirst() {
        Producto product = product("Yogur", new BigDecimal("900.00"));

        Optional<SchoolAssistantFixedReply> reply = fixedTopics.respond(
                "cancelar mi pedido",
                studentContext(new BigDecimal("5000.00")),
                List.of(assistantMessage(pendingMetadata(product.getId(), 1)))
        );

        assertTrue(reply.isPresent());
        assertEquals("Listo, descarte esa compra pendiente.", reply.orElseThrow().answer());
        assertEquals(SchoolAssistantActionType.COMPRA_ANTICIPADA,
                reply.orElseThrow().action().type());
        assertEquals(SchoolAssistantActionStatus.CANCELADA,
                reply.orElseThrow().action().status());
        verifyNoInteractions(cancelPresentialPurchase);
    }

    @Test
    void purchaseCancellationCancelsRecentlyCreatedPurchaseFromConversation() {
        Producto product = product("Yogur", new BigDecimal("900.00"));
        UUID purchaseId = UUID.randomUUID();
        Purchase cancelledPurchase = purchase(product, 1, NOW);
        cancelledPurchase.setId(purchaseId);
        cancelledPurchase.setStudent(alumno(studentId));
        cancelledPurchase.setStatus(EstadoCompra.CANCELADO);

        when(cancelPresentialPurchase.execute(
                argThat(request -> request.purchaseId().equals(purchaseId)
                        && request.studentId().equals(studentId))
        )).thenReturn(cancelResponse(cancelledPurchase));

        Optional<SchoolAssistantFixedReply> reply = fixedTopics.respond(
                "cancelar mi compra",
                studentContext(new BigDecimal("5000.00")),
                List.of(assistantMessage(createdPurchaseMetadata(purchaseId)))
        );

        assertTrue(reply.isPresent());
        SchoolAssistantFixedReply answer = reply.orElseThrow();
        assertEquals("Listo, cancele tu compra y te devolvi $900.00 en creditos.",
                answer.answer());
        assertEquals(SchoolAssistantIntent.CANCEL_PURCHASE, answer.intent());
        assertEquals(SchoolAssistantActionType.CANCELACION_COMPRA, answer.action().type());
        assertEquals(SchoolAssistantActionStatus.EJECUTADA, answer.action().status());
        assertEquals(EstadoCompra.CANCELADO, answer.action().purchaseStatus());
        assertEquals(purchaseId, answer.action().purchaseId());
    }

    @Test
    void purchaseCancellationCancelsOnlyCancelablePurchaseFromHistory() {
        Producto product = product("Yogur", new BigDecimal("900.00"));
        UUID purchaseId = UUID.randomUUID();
        Purchase pendingPurchase = purchase(product, 1, NOW);
        pendingPurchase.setId(purchaseId);
        pendingPurchase.setStudent(alumno(studentId));
        pendingPurchase.setStatus(EstadoCompra.PENDIENTE);
        Purchase cancelledPurchase = purchase(product, 1, NOW);
        cancelledPurchase.setId(purchaseId);
        cancelledPurchase.setStudent(alumno(studentId));
        cancelledPurchase.setStatus(EstadoCompra.CANCELADO);

        when(purchaseRepository.findHistorialByAlumno(studentId))
                .thenReturn(List.of(pendingPurchase));
        when(cancelPresentialPurchase.execute(
                argThat(request -> request.purchaseId().equals(purchaseId)
                        && request.studentId().equals(studentId))
        )).thenReturn(cancelResponse(cancelledPurchase));

        Optional<SchoolAssistantFixedReply> reply = fixedTopics.respond(
                "cancelar mi pedido",
                studentContext(new BigDecimal("5000.00")),
                List.of()
        );

        assertTrue(reply.isPresent());
        assertEquals(SchoolAssistantActionType.CANCELACION_COMPRA,
                reply.orElseThrow().action().type());
        assertEquals(SchoolAssistantActionStatus.EJECUTADA,
                reply.orElseThrow().action().status());
    }

    @Test
    void purchaseCancellationWithMultipleCancelablePurchasesDoesNotCancelAutomatically() {
        Producto product = product("Yogur", new BigDecimal("900.00"));
        Purchase firstPurchase = purchase(product, 1, NOW.minusMinutes(10));
        firstPurchase.setId(UUID.randomUUID());
        firstPurchase.setStatus(EstadoCompra.PENDIENTE);
        Purchase secondPurchase = purchase(product, 2, NOW.minusMinutes(5));
        secondPurchase.setId(UUID.randomUUID());
        secondPurchase.setStatus(EstadoCompra.PENDIENTE);

        when(purchaseRepository.findHistorialByAlumno(studentId))
                .thenReturn(List.of(firstPurchase, secondPurchase));

        Optional<SchoolAssistantFixedReply> reply = fixedTopics.respond(
                "cancelar mi pedido",
                studentContext(new BigDecimal("5000.00")),
                List.of()
        );

        assertTrue(reply.isPresent());
        assertTrue(reply.orElseThrow().answer().contains("Tenes mas de una compra pendiente"));
        assertEquals(SchoolAssistantActionStatus.INFORMATIVA,
                reply.orElseThrow().action().status());
        verifyNoInteractions(cancelPresentialPurchase);
    }

    @Test
    void cancellationWithoutPendingActionExplainsThereIsNothingToCancel() {
        Optional<SchoolAssistantFixedReply> reply = fixedTopics.respond(
                "cancelar",
                studentContext(new BigDecimal("5000.00")),
                List.of()
        );

        assertTrue(reply.isPresent());
        assertEquals("No tengo ninguna compra pendiente para cancelar.",
                reply.orElseThrow().answer());
    }

    @Test
    void kiosqueroPendingOrdersUsesRepositoryStatusesAndLimit() {
        Producto product = product("Cafe", new BigDecimal("600.00"));
        Purchase purchase = purchase(product, 1, NOW);
        purchase.setStatus(EstadoCompra.EN_PREPARACION);
        when(purchaseRepository.findByBuffetAndStatuses(
                eq(buffetId),
                eq(List.of(
                        EstadoCompra.PENDIENTE,
                        EstadoCompra.EN_PREPARACION,
                        EstadoCompra.LISTO
                )),
                eq(3)
        )).thenReturn(List.of(purchase));

        Optional<SchoolAssistantFixedReply> reply = fixedTopics.respond(
                "estado de pedido",
                kiosqueroContext(),
                List.of()
        );

        assertTrue(reply.isPresent());
        assertTrue(reply.orElseThrow().answer().contains("Pedidos pendientes"));
        assertTrue(reply.orElseThrow().answer().contains("Cafe"));
    }

    @Test
    void tutorWithoutChildrenUsesEmptyFactsAnswer() {
        SchoolAssistantContext context = new SchoolAssistantContext(
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

        Optional<SchoolAssistantFixedReply> reply = fixedTopics.respond(
                "hijos",
                context,
                List.of()
        );

        assertTrue(reply.isPresent());
        assertEquals("No veo hijos asociados a tu cuenta.", reply.orElseThrow().answer());
    }

    @Test
    void unknownStudentMessageReturnsEmpty() {
        Optional<SchoolAssistantFixedReply> reply = fixedTopics.respond(
                "necesito ayuda con otra cosa",
                studentContext(new BigDecimal("5000.00")),
                List.of()
        );

        assertTrue(reply.isEmpty());
    }

    @Test
    void unsupportedStudentIntentReturnsEmpty() {
        Optional<SchoolAssistantFixedReply> reply = fixedTopics.respond(
                "ver presupuestos",
                studentContext(new BigDecimal("5000.00")),
                List.of()
        );

        assertTrue(reply.isEmpty());
    }

    @Test
    void unsupportedTutorIntentReturnsEmpty() {
        Optional<SchoolAssistantFixedReply> reply = fixedTopics.respond(
                "saldo disponible",
                tutorContext(),
                List.of()
        );

        assertTrue(reply.isEmpty());
    }

    @Test
    void tutorStockQuestionReturnsEmpty() {
        Optional<SchoolAssistantFixedReply> reply = fixedTopics.respond(
                "ver stock",
                tutorContext(),
                List.of()
        );

        assertTrue(reply.isEmpty());
    }

    @Test
    void kiosqueroChildrenQuestionReturnsEmpty() {
        Optional<SchoolAssistantFixedReply> reply = fixedTopics.respond(
                "mis hijos",
                kiosqueroContext(),
                List.of()
        );

        assertTrue(reply.isEmpty());
    }

    @Test
    void kiosqueroPendingOrdersWithoutPurchasesUsesEmptyAnswer() {
        when(purchaseRepository.findByBuffetAndStatuses(
                eq(buffetId),
                eq(List.of(
                        EstadoCompra.PENDIENTE,
                        EstadoCompra.EN_PREPARACION,
                        EstadoCompra.LISTO
                )),
                eq(3)
        )).thenReturn(List.of());

        Optional<SchoolAssistantFixedReply> reply = fixedTopics.respond(
                "pedidos pendientes",
                kiosqueroContext(),
                List.of()
        );

        assertTrue(reply.isPresent());
        assertEquals("No hay pedidos pendientes en el buffet por ahora.",
                reply.orElseThrow().answer());
    }

    @Test
    void stockWithoutProductsUsesEmptyAnswer() {
        when(productRepository.findAllActiveWithStock(buffetId)).thenReturn(List.of());

        Optional<SchoolAssistantFixedReply> reply = fixedTopics.respond(
                "inventario",
                kiosqueroContext(),
                List.of()
        );

        assertTrue(reply.isPresent());
        assertEquals("No veo productos activos con stock por ahora.",
                reply.orElseThrow().answer());
    }

    @Test
    void tutorBudgetsWithoutChildrenUsesEmptyFactsAnswer() {
        SchoolAssistantContext context = new SchoolAssistantContext(
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

        Optional<SchoolAssistantFixedReply> reply = fixedTopics.respond(
                "presupuestos",
                context,
                List.of()
        );

        assertTrue(reply.isPresent());
        assertEquals("No veo datos de hijos asociados a tu cuenta.",
                reply.orElseThrow().answer());
    }

    @Test
    void eventsWithoutEventsUsesEmptyAnswer() {
        Optional<SchoolAssistantFixedReply> reply = fixedTopics.respond(
                "eventos",
                studentContext(new BigDecimal("5000.00")),
                List.of()
        );

        assertTrue(reply.isPresent());
        assertEquals("No veo eventos escolares proximos por ahora.",
                reply.orElseThrow().answer());
    }

    @Test
    void menuWithPreviousBuffetButNoAvailableProductsUsesEmptyAnswer() {
        Producto previousProduct = product("Pancho", new BigDecimal("1000.00"));
        Purchase purchase = purchase(previousProduct, 1, NOW.minusDays(1));
        Producto unavailable = product("Tarta", new BigDecimal("1200.00"));
        unavailable.setStockActual(0);
        when(purchaseRepository.findHistorialByAlumno(studentId)).thenReturn(List.of(purchase));
        when(productRepository.findAllActiveWithStock(buffetId)).thenReturn(List.of(unavailable));

        Optional<SchoolAssistantFixedReply> reply = fixedTopics.respond(
                "menu",
                studentContext(new BigDecimal("5000.00")),
                List.of()
        );

        assertTrue(reply.isPresent());
        assertEquals("No veo productos disponibles para ese buffet por ahora.",
                reply.orElseThrow().answer());
    }

    @Test
    void studentPendingOrdersWithoutPendingPurchasesUsesEmptyAnswer() {
        Producto product = product("Alfajor", new BigDecimal("700.00"));
        Purchase purchase = purchase(product, 1, NOW.minusDays(1));
        when(purchaseRepository.findHistorialByAlumno(studentId)).thenReturn(List.of(purchase));

        Optional<SchoolAssistantFixedReply> reply = fixedTopics.respond(
                "pedidos pendientes",
                studentContext(new BigDecimal("5000.00")),
                List.of()
        );

        assertTrue(reply.isPresent());
        assertEquals("No tenes pedidos pendientes.", reply.orElseThrow().answer());
    }

    @Test
    void withdrawalCodeWithoutAvailableCodeUsesEmptyAnswer() {
        Producto product = product("Jugo", new BigDecimal("800.00"));
        Purchase purchase = purchase(product, 1, NOW.minusMinutes(15));
        purchase.setStatus(EstadoCompra.LISTO);
        purchase.setWithdrawalCode(" ");
        when(purchaseRepository.findHistorialByAlumno(studentId)).thenReturn(List.of(purchase));

        Optional<SchoolAssistantFixedReply> reply = fixedTopics.respond(
                "codigo de retiro",
                studentContext(new BigDecimal("5000.00")),
                List.of()
        );

        assertTrue(reply.isPresent());
        assertEquals("No veo ningun pedido pendiente con codigo de retiro.",
                reply.orElseThrow().answer());
    }

    @Test
    void repeatLastPurchaseWithoutRebuyableHistoryUsesFailedAnswer() {
        Purchase purchase = new Purchase();
        purchase.setStatus(EstadoCompra.ENTREGADO);
        purchase.setItems(null);
        when(purchaseRepository.findHistorialByAlumno(studentId)).thenReturn(List.of(purchase));

        Optional<SchoolAssistantFixedReply> reply = fixedTopics.respond(
                "repeti mi ultima compra",
                studentContext(new BigDecimal("5000.00")),
                List.of()
        );

        assertTrue(reply.isPresent());
        assertEquals("Todavia no encontre una compra tuya que pueda repetir.",
                reply.orElseThrow().answer());
    }

    @Test
    void buyUsualPurchaseWithoutPatternUsesFailedAnswer() {
        when(purchaseRepository.findHistorialByAlumno(studentId)).thenReturn(List.of());

        Optional<SchoolAssistantFixedReply> reply = fixedTopics.respond(
                "comprame lo de siempre",
                studentContext(new BigDecimal("5000.00")),
                List.of()
        );

        assertTrue(reply.isPresent());
        assertEquals("Todavia no tengo suficientes compras tuyas para armar "
                        + "una compra habitual.",
                reply.orElseThrow().answer());
    }

    @Test
    void confirmWithoutPendingActionUsesFailedAnswer() {
        Optional<SchoolAssistantFixedReply> reply = fixedTopics.respond(
                "confirmar",
                studentContext(new BigDecimal("5000.00")),
                List.of()
        );

        assertTrue(reply.isPresent());
        assertEquals("No tengo ninguna compra pendiente para confirmar.",
                reply.orElseThrow().answer());
    }

    @Test
    void selectRecessWithoutPendingActionUsesFailedAnswer() {
        Optional<SchoolAssistantFixedReply> reply = fixedTopics.respond(
                "segundo recreo",
                studentContext(new BigDecimal("5000.00")),
                List.of()
        );

        assertTrue(reply.isPresent());
        assertEquals("No tengo ninguna compra pendiente para elegir recreo.",
                reply.orElseThrow().answer());
    }

    @Test
    void confirmPendingPurchaseFromAnotherStudentFails() {
        Producto product = product("Yogur", new BigDecimal("900.00"));

        Optional<SchoolAssistantFixedReply> reply = fixedTopics.respond(
                "confirmar",
                studentContext(new BigDecimal("5000.00")),
                List.of(assistantMessage(pendingMetadata(
                        UUID.randomUUID(),
                        product.getId(),
                        1,
                        RecessTime.FIRST_RECESS
                )))
        );

        assertTrue(reply.isPresent());
        assertEquals("Esa compra pendiente no corresponde a tu usuario.",
                reply.orElseThrow().answer());
    }

    @Test
    void selectRecessForPendingPurchaseFromAnotherStudentFails() {
        Producto product = product("Yogur", new BigDecimal("900.00"));

        Optional<SchoolAssistantFixedReply> reply = fixedTopics.respond(
                "salida",
                studentContext(new BigDecimal("5000.00")),
                List.of(assistantMessage(pendingMetadata(
                        UUID.randomUUID(),
                        product.getId(),
                        1,
                        null
                )))
        );

        assertTrue(reply.isPresent());
        assertEquals("Esa compra pendiente no corresponde a tu usuario.",
                reply.orElseThrow().answer());
    }

    @Test
    void confirmPendingPurchaseWithUnavailableProductFails() {
        UUID productId = UUID.randomUUID();
        when(productRepository.findActiveById(productId)).thenReturn(Optional.empty());

        Optional<SchoolAssistantFixedReply> reply = fixedTopics.respond(
                "confirmar",
                studentContext(new BigDecimal("5000.00")),
                List.of(assistantMessage(pendingMetadata(
                        productId,
                        1,
                        LocalDate.of(2026, 5, 28),
                        RecessTime.FIRST_RECESS
                )))
        );

        assertTrue(reply.isPresent());
        assertEquals("No pude realizar la compra porque algun producto ya no esta "
                        + "disponible.",
                reply.orElseThrow().answer());
    }

    @Test
    void repeatLastPurchaseWithUnavailableProductDoesNotCreatePlan() {
        Producto product = product("Agua", new BigDecimal("700.00"));
        Purchase purchase = purchase(product, 1, NOW.minusDays(1));
        when(purchaseRepository.findHistorialByAlumno(studentId)).thenReturn(List.of(purchase));
        when(productRepository.findActiveById(product.getId())).thenReturn(Optional.empty());

        Optional<SchoolAssistantFixedReply> reply = fixedTopics.respond(
                "repeti mi ultima compra",
                studentContext(new BigDecimal("5000.00")),
                List.of()
        );

        assertTrue(reply.isPresent());
        assertEquals("Todavia no encontre una compra tuya que pueda repetir.",
                reply.orElseThrow().answer());
    }

    @Test
    void buyUsualPurchaseWithoutBuffetDoesNotCreatePlan() {
        Producto product = product("Barrita", new BigDecimal("650.00"));
        Purchase firstPurchase = purchase(product, 1, NOW.minusDays(1));
        Purchase secondPurchase = purchase(product, 1, NOW.minusDays(2));
        firstPurchase.setBuffetId(null);
        secondPurchase.setBuffetId(null);

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
        assertEquals("Todavia no tengo suficientes compras tuyas para armar "
                        + "una compra habitual.",
                reply.orElseThrow().answer());
    }

    @Test
    void frequentPurchasesWithPatternsListsSortedPatterns() {
        Producto first = product("Agua", new BigDecimal("700.00"));
        Producto second = product("Alfajor", new BigDecimal("500.00"));
        Producto third = product("Jugo", new BigDecimal("800.00"));

        when(purchaseRepository.findHistorialByAlumno(studentId)).thenReturn(List.of(
                purchase(first, 1, NOW.minusDays(3)),
                purchase(second, 1, NOW.minusHours(1)),
                purchase(third, 1, NOW.minusHours(2)),
                purchase(first, 1, NOW.minusDays(1)),
                purchase(second, 1, NOW.minusDays(4)),
                purchase(third, 1, NOW.minusDays(5)),
                purchase(first, 1, NOW.minusDays(2))
        ));
        when(productRepository.findById(first.getId())).thenReturn(Optional.of(first));
        when(productRepository.findById(second.getId())).thenReturn(Optional.of(second));
        when(productRepository.findById(third.getId())).thenReturn(Optional.of(third));

        Optional<SchoolAssistantFixedReply> reply = fixedTopics.respond(
                "que compro seguido",
                studentContext(new BigDecimal("5000.00")),
                List.of()
        );

        assertTrue(reply.isPresent());
        String answer = reply.orElseThrow().answer();
        assertTrue(answer.contains("Tus compras frecuentes"));
        assertTrue(answer.indexOf("Agua") < answer.indexOf("Alfajor"));
        assertTrue(answer.indexOf("Alfajor") < answer.indexOf("Jugo"));
        assertTrue(answer.contains("3 veces"));
    }

    @Test
    void stockProductWithMissingFieldsUsesFormatterFallbacks() {
        Producto product = product(null, null);
        product.setStockActual(null);
        when(productRepository.findAllActiveWithStock(buffetId)).thenReturn(List.of(product));

        Optional<SchoolAssistantFixedReply> reply = fixedTopics.respond(
                "stock",
                kiosqueroContext(),
                List.of()
        );

        assertTrue(reply.isPresent());
        assertTrue(reply.orElseThrow().answer().contains("Producto por $0.00"));
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
                List.of("hijo=Mateo Perez|id=11111111-1111-1111-1111-111111111111"
                        + "|saldo=900.00|grado=2do B"
                        + "|presupuesto=monto=2000.00,periodo=MENSUAL,"
                        + "fechaInicio=2026-06-07"
                        + "|restriccionesSalud=[Sin TACC]"
                        + "|productosBloqueados=[Gaseosa]"),
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

    private Producto product(String name, BigDecimal price, int stock) {
        Producto product = product(name, price);
        product.setStockActual(stock);
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

    private Alumno alumno(UUID id) {
        Alumno alumno = new Alumno();
        alumno.setId(id);
        return alumno;
    }

    private boolean matchesPurchaseRequest(
            CreateAssistantAdvancePurchaseRequest request,
            Producto product,
            int quantity,
            RecessTime recessTime,
            LocalDate pickupDate
    ) {
        return request != null
                && studentId.equals(request.studentId())
                && buffetId.equals(request.buffetId())
                && request.cart().containsKey(product)
                && request.cart().containsValue(quantity)
                && recessTime == request.recessTime()
                && pickupDate.equals(request.pickupDate());
    }

    private CreateAssistantAdvancePurchaseResponse responseFrom(Purchase purchase) {
        return new CreateAssistantAdvancePurchaseResponse(
                purchase.getId(),
                purchase.studentIdOrNull(),
                purchase.getBuffetId(),
                purchase.getStatus(),
                purchase.getWithdrawalCode(),
                purchase.getTotalAmount(),
                purchase.getPickupDate(),
                purchase.getPickupSlotId(),
                purchase.getDate(),
                purchase.getItems()
                        .entrySet()
                        .stream()
                        .map(entry -> purchaseItem(entry.getKey(), entry.getValue()))
                        .toList()
        );
    }

    private CreateAssistantAdvancePurchaseResponse.Item purchaseItem(
            Producto product,
            Integer quantity
    ) {
        return new CreateAssistantAdvancePurchaseResponse.Item(
                product.getId(),
                product.getNombre(),
                quantity,
                product.getPrecio()
        );
    }

    private CancelPresentialPurchaseResponse cancelResponse(Purchase purchase) {
        return new CancelPresentialPurchaseResponse(
                purchase.getId(),
                purchase.studentIdOrNull(),
                purchase.getBuffetId(),
                purchase.getStatus(),
                purchase.getWithdrawalCode(),
                purchase.getTotalAmount()
        );
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
        return pendingMetadata(productId, quantity, null, null);
    }

    private Map<String, Object> createdPurchaseMetadata(UUID purchaseId) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("estadoAccionChatbot", "EJECUTADA");
        metadata.put("compraId", purchaseId.toString());
        return metadata;
    }

    private Map<String, Object> pendingMetadata(
            UUID productId,
            int quantity,
            RecessTime recessTime
    ) {
        return pendingMetadata(productId, quantity, null, recessTime);
    }

    private Map<String, Object> pendingMetadata(
            UUID productId,
            int quantity,
            LocalDate pickupDate,
            RecessTime recessTime
    ) {
        return pendingMetadata(studentId, productId, quantity, pickupDate, recessTime);
    }

    private Map<String, Object> pendingMetadata(
            UUID ownerStudentId,
            UUID productId,
            int quantity,
            RecessTime recessTime
    ) {
        return pendingMetadata(ownerStudentId, productId, quantity, null, recessTime);
    }

    private Map<String, Object> pendingMetadata(
            UUID ownerStudentId,
            UUID productId,
            int quantity,
            LocalDate pickupDate,
            RecessTime recessTime
    ) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("estadoAccionChatbot", "PENDIENTE");
        metadata.put("accionPendienteChatbot", pendingAction(
                ownerStudentId,
                productId,
                quantity,
                pickupDate,
                recessTime
        ));
        return metadata;
    }

    private Map<String, Object> pendingAction(
            UUID ownerStudentId,
            UUID productId,
            int quantity,
            LocalDate pickupDate,
            RecessTime recessTime
    ) {
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("tipo", "COMPRA_RAPIDA");
        action.put("alumnoId", ownerStudentId.toString());
        action.put("buffetId", buffetId.toString());
        if (pickupDate != null)
            action.put("fechaRetiro", pickupDate.toString());
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
