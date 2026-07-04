package com.recred.backend.unit.application;

import com.recred.backend.application.AskSchoolAssistant;
import com.recred.backend.application.AskSchoolAssistantRequest;
import com.recred.backend.application.AskSchoolAssistantResponse;
import com.recred.backend.application.SchoolAssistantFixedReply;
import com.recred.backend.application.SchoolAssistantFixedTopics;
import com.recred.backend.domain.exceptions.AlumnoNoEncontradoException;
import com.recred.backend.domain.exceptions.ChatSessionNotFoundException;
import com.recred.backend.domain.exceptions.InvalidChatMessageException;
import com.recred.backend.domain.exceptions.KiosqueroNotFoundException;
import com.recred.backend.domain.exceptions.TutorNoEncontradoException;
import com.recred.backend.domain.exceptions.UsuarioAutenticadoNoEncontradoException;
import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Billetera;
import com.recred.backend.domain.models.ChatMessage;
import com.recred.backend.domain.models.ChatSession;
import com.recred.backend.domain.models.ClasificacionSalud;
import com.recred.backend.domain.models.EstadoChatSesion;
import com.recred.backend.domain.models.EstadoCompra;
import com.recred.backend.domain.models.EstadoPagoExterno;
import com.recred.backend.domain.models.Grado;
import com.recred.backend.domain.models.MedioPago;
import com.recred.backend.domain.models.PresupuestoAlumno;
import com.recred.backend.domain.models.Producto;
import com.recred.backend.domain.models.Purchase;
import com.recred.backend.domain.models.RecessTime;
import com.recred.backend.domain.models.RolChatMensaje;
import com.recred.backend.domain.models.SchoolAssistantAction;
import com.recred.backend.domain.models.SchoolAssistantActionStatus;
import com.recred.backend.domain.models.SchoolAssistantActionType;
import com.recred.backend.domain.models.SchoolCalendarEvent;
import com.recred.backend.domain.models.SchoolAssistantDraft;
import com.recred.backend.domain.models.SchoolAssistantIntent;
import com.recred.backend.domain.models.SchoolAssistantPayment;
import com.recred.backend.domain.models.SchoolAssistantPrompt;
import com.recred.backend.domain.models.SchoolAssistantPurchase;
import com.recred.backend.domain.models.SchoolAssistantRequiredInput;
import com.recred.backend.domain.models.SchoolAssistantRole;
import com.recred.backend.domain.models.SchoolAssistantSuggestion;
import com.recred.backend.domain.models.TipoDiaEscolar;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.ChatMessageRepository;
import com.recred.backend.domain.repositories.ChatSessionRepository;
import com.recred.backend.domain.repositories.KiosqueroRepository;
import com.recred.backend.domain.repositories.PresupuestoRepository;
import com.recred.backend.domain.repositories.ProductRepository;
import com.recred.backend.domain.repositories.PurchaseRepository;
import com.recred.backend.domain.repositories.SchoolAssistantPaymentRepository;
import com.recred.backend.domain.repositories.SchoolAssistantPurchaseRepository;
import com.recred.backend.domain.repositories.SchoolCalendarRepository;
import com.recred.backend.domain.repositories.TutorRepository;
import com.recred.backend.domain.repositories.UsuarioRepository;
import com.recred.backend.domain.repositories.WalletRepository;
import com.recred.backend.domain.services.SchoolAssistantResponder;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AskSchoolAssistantTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-05-27T12:00:00Z"),
            ZoneOffset.UTC
    );
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 27, 12, 0);
    private static final String STUDENT_SUBJECT = "student-sub";
    private static final String TUTOR_SUBJECT = "tutor-sub";
    private static final String KIOSQUERO_SUBJECT = "kiosquero-sub";

    @Mock
    private AlumnoRepository alumnoRepository;
    @Mock
    private WalletRepository walletRepository;
    @Mock
    private SchoolAssistantPaymentRepository paymentRepository;
    @Mock
    private SchoolAssistantPurchaseRepository purchaseRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private TutorRepository tutorRepository;
    @Mock
    private KiosqueroRepository kiosqueroRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private PurchaseRepository purchaseHistoryRepository;
    @Mock
    private PresupuestoRepository presupuestoRepository;
    @Mock
    private SchoolCalendarRepository schoolCalendarRepository;
    @Mock
    private ChatSessionRepository chatSessionRepository;
    @Mock
    private ChatMessageRepository chatMessageRepository;
    @Mock
    private SchoolAssistantResponder schoolAssistantResponder;
    @Mock
    private SchoolAssistantFixedTopics fixedTopics;

    private AskSchoolAssistant askSchoolAssistant;
    private UUID studentId;
    private UUID sessionId;
    private ChatSession session;

    @BeforeEach
    void setUp() {
        askSchoolAssistant = new AskSchoolAssistant(
                alumnoRepository,
                walletRepository,
                paymentRepository,
                purchaseRepository,
                schoolCalendarRepository,
                chatSessionRepository,
                chatMessageRepository,
                schoolAssistantResponder,
                usuarioRepository,
                tutorRepository,
                kiosqueroRepository,
                productRepository,
                purchaseHistoryRepository,
                presupuestoRepository,
                fixedTopics,
                CLOCK
        );
        lenient().when(fixedTopics.respond(any(), any(), any()))
                .thenReturn(Optional.empty());
        studentId = UUID.randomUUID();
        sessionId = UUID.randomUUID();
        session = new ChatSession(sessionId, EstadoChatSesion.ABIERTA, NOW, NOW);
    }

    @Test
    void execute_WhenStudentAsks_ShouldBuildFullContextAndReturnAiResponse() {
        SchoolAssistantPayment payment = payment();
        SchoolAssistantPurchase purchase = purchase();
        var event = event();

        givenAuthenticatedStudent(studentId);
        givenNewStudentSession();
        givenRecentMessages(List.of());
        givenStudentContext(
                Optional.of(wallet(new BigDecimal("1500.00"))),
                List.of(payment),
                List.of(purchase),
                List.of(event)
        );
        givenAiAnswer("Respuesta IA.", "gemini-test");

        AskSchoolAssistantResponse response = whenStudentAsks(
                "Quiero ver saldo, pagos, compras y eventos");

        assertEquals(sessionId, response.sessionId());
        assertEquals("Respuesta IA.", response.answer());
        assertTrue(response.generatedByAi());
        assertEquals("gemini-test", response.model());
        assertEquals(List.of("SALDO", "PAGOS", "COMPRAS", "EVENTOS"), response.capabilities());
        assertEquals("ALUMNO", response.role());
        assertEquals("UNKNOWN", response.intent());
        assertFalse(response.suggestions().isEmpty());

        SchoolAssistantPrompt prompt = thenAiPromptWasSent();
        assertEquals("Quiero ver saldo, pagos, compras y eventos", prompt.question());
        assertEquals(studentId, prompt.context().studentId());
        assertEquals(new BigDecimal("1500.00"), prompt.context().balance().orElseThrow());
        assertEquals(payment, prompt.context().recentPayments().get(0));
        assertEquals(purchase, prompt.context().recentPurchases().get(0));
        assertEquals(event, prompt.context().upcomingEvents().get(0));
        assertTrue(prompt.recentMessages().isEmpty());
        thenMessagesWereStored("Respuesta IA.", true);
    }

    @Test
    void execute_WhenAiAnswerNeedsFormatting_ShouldReturnAndStoreFormattedAnswer() {
        String rawAnswer = "Stock actual: Coca-Cola 500ml por $1200.00 (stock: 50); "
                + "Jugo de Naranja 300ml por $950.00 (stock: 35); "
                + "Papas Chips 50g por $600.00 (stock: 70).";
        String expectedAnswer = """
                Stock actual:
                - Coca-Cola 500ml por $1200.00 (stock: 50)
                - Jugo de Naranja 300ml por $950.00 (stock: 35)
                - Papas Chips 50g por $600.00 (stock: 70)
                """.trim();

        givenAuthenticatedStudent(studentId);
        givenNewStudentSession();
        givenRecentMessages(List.of());
        givenEmptyStudentContext();
        givenAiAnswer(rawAnswer, "gemini-test");

        AskSchoolAssistantResponse response = whenStudentAsks("stock");

        assertEquals(expectedAnswer, response.answer());
        assertFalse(response.answer().contains(";"));
        thenMessagesWereStored(expectedAnswer, true);
    }

    @Test
    void execute_WhenStudentIsResolvedFromAuthenticatedUser_ShouldStartStudentSession() {
        UUID userId = UUID.randomUUID();
        givenAuthenticatedUser(STUDENT_SUBJECT, userId);
        givenStudentResolvedFromUser(userId);
        givenNewStudentSession();
        givenRecentMessages(List.of());
        givenEmptyStudentContext();
        givenNoAiAnswer();

        AskSchoolAssistantResponse response = whenStudentAsks("saldo");

        assertEquals(sessionId, response.sessionId());
        verify(chatSessionRepository).startForStudent(studentId, NOW);
        verify(walletRepository).findByStudentId(studentId);
    }

    @Test
    void execute_WhenAiDoesNotReturnAnswer_ShouldUseStudentFallback() {
        givenAuthenticatedStudent(studentId);
        givenNewStudentSession();
        givenRecentMessages(List.of());
        givenEmptyStudentContext();
        givenNoAiAnswer();

        AskSchoolAssistantResponse response = whenStudentAsks("cuanto saldo tengo");

        assertEquals(
                "No pude responder ahora. Puedo ayudarte con saldos, pagos, compras "
                        + "y eventos escolares.",
                response.answer()
        );
        assertFalse(response.generatedByAi());
        assertNull(response.model());
        assertFalse(response.suggestions().isEmpty());
        verify(schoolAssistantResponder).respond(any());
    }

    @Test
    void execute_WhenFixedReplyExists_ShouldUseItBeforeAiAndStoreMetadata() {
        Map<String, Object> metadata = Map.of("intencionChatbot", "BALANCE");
        givenAuthenticatedStudent(studentId);
        givenNewStudentSession();
        givenRecentMessages(List.of());
        givenEmptyStudentContext();
        givenFixedReply("saldo", new SchoolAssistantFixedReply(
                "Tenes $1500.00 disponibles.",
                metadata
        ));

        AskSchoolAssistantResponse response = whenStudentAsks("saldo");

        assertEquals("Tenes $1500.00 disponibles.", response.answer());
        assertFalse(response.generatedByAi());
        verify(schoolAssistantResponder, never()).respond(any());
        Map<?, ?> storedMetadata = storedAssistantMetadata();
        assertEquals("BALANCE", storedMetadata.get("intencionChatbot"));
        assertEquals(false, storedMetadata.get("generadoPorIa"));
    }

    @Test
    void execute_WhenMessageIsBlank_ShouldThrowException() {
        givenAuthenticatedStudent(studentId);

        Executable execution = () -> whenStudentAsks(" ");

        assertThrows(InvalidChatMessageException.class, execution);

        verify(chatSessionRepository, never()).startForStudent(any(), any());
    }

    @Test
    void execute_WhenMessageIsNull_ShouldThrowException() {
        givenAuthenticatedStudent(studentId);

        Executable execution = () -> whenStudentAsks(null);

        assertThrows(InvalidChatMessageException.class, execution);

        verify(chatSessionRepository, never()).startForStudent(any(), any());
    }

    @Test
    void execute_WhenStudentCannotBeResolved_ShouldThrowException() {
        UUID userId = UUID.randomUUID();
        givenAuthenticatedUser(STUDENT_SUBJECT, userId);
        when(alumnoRepository.existsById(userId)).thenReturn(false);
        when(alumnoRepository.findStudentIdByUserId(userId)).thenReturn(Optional.empty());

        Executable execution = () -> whenStudentAsks("saldo");

        assertThrows(AlumnoNoEncontradoException.class, execution);
    }

    @Test
    void execute_WhenStudentSessionDoesNotExist_ShouldThrowException() {
        givenAuthenticatedStudent(studentId);
        when(chatSessionRepository.findOpenByIdAndStudentId(sessionId, studentId))
                .thenReturn(Optional.empty());

        Executable execution = () -> whenStudentAsks(sessionId, "saldo");

        assertThrows(ChatSessionNotFoundException.class, execution);

        verify(chatMessageRepository, never()).save(any());
    }

    @Test
    void execute_WhenStudentSessionExists_ShouldUseIt() {
        givenAuthenticatedStudent(studentId);
        when(chatSessionRepository.findOpenByIdAndStudentId(sessionId, studentId))
                .thenReturn(Optional.of(session));
        givenRecentMessages(List.of());
        givenEmptyStudentContext();
        givenNoAiAnswer();

        AskSchoolAssistantResponse response = whenStudentAsks(sessionId, "saldo");

        assertEquals(sessionId, response.sessionId());
        verify(chatSessionRepository, never()).startForStudent(any(), any());
        verify(chatSessionRepository).findOpenByIdAndStudentId(sessionId, studentId);
    }

    @Test
    void execute_WhenTutorAsks_ShouldBuildTutorContext() {
        UUID userId = UUID.randomUUID();
        UUID tutorId = UUID.randomUUID();
        Alumno child = child("Mateo", "Perez", new BigDecimal("900.00"));
        Tutor tutor = tutor(tutorId, Set.of(child));
        PresupuestoAlumno budget = new PresupuestoAlumno();
        budget.setMontoLimiteGeneral(new BigDecimal("2000.00"));
        Purchase purchase = tutorPurchase(child);

        givenAuthenticatedTutor(userId, tutor);
        givenNewUserSession(userId);
        givenRecentMessages(List.of());
        when(presupuestoRepository.findActivoByAlumnoId(child.getId()))
                .thenReturn(Optional.of(budget));
        when(purchaseHistoryRepository.findHistorialByTutor(tutorId)).thenReturn(List.of(purchase));
        when(paymentRepository.findRecentByStudentId(child.getId(), 2)).thenReturn(List.of());
        when(schoolCalendarRepository.findByStudentIdAndDateRange(
                child.getId(),
                LocalDate.of(2026, 5, 27),
                LocalDate.of(2026, 6, 26)
        )).thenReturn(List.of());
        givenAiAnswer("Mateo tiene $900.00.", "gemini-test");

        AskSchoolAssistantResponse response = whenTutorAsks("saldo de mis hijos");

        assertEquals("Mateo tiene $900.00.", response.answer());
        assertTrue(response.capabilities().contains("HIJOS"));
        assertTrue(response.capabilities().contains("PRESUPUESTOS"));
        verify(chatSessionRepository).startForUser(userId, NOW);

        SchoolAssistantPrompt prompt = thenAiPromptWasSent();
        assertEquals("TUTOR", prompt.context().role().name());
        assertTrue(prompt.context().facts().stream().anyMatch(fact ->
                fact.contains("hijo=Mateo Perez") && fact.contains("saldo=900.00")));
    }

    @Test
    void execute_WhenTutorUsesExistingSessionAndAiDoesNotAnswer_ShouldUseTutorFallback() {
        UUID userId = UUID.randomUUID();
        UUID tutorId = UUID.randomUUID();
        Tutor tutor = tutor(tutorId, Set.of());
        givenAuthenticatedTutor(userId, tutor);
        when(chatSessionRepository.findOpenByIdAndUserId(sessionId, userId))
                .thenReturn(Optional.of(session));
        givenRecentMessages(List.of());
        when(purchaseHistoryRepository.findHistorialByTutor(tutorId)).thenReturn(List.of());
        givenNoAiAnswer();

        AskSchoolAssistantResponse response = whenTutorAsks(sessionId, "otra consulta");

        assertEquals(
                "No pude responder ahora. Puedo ayudarte con saldos, pagos, compras, "
                        + "eventos, presupuestos y restricciones de tus hijos.",
                response.answer()
        );
        assertEquals("TUTOR", response.role());
        verify(chatSessionRepository).findOpenByIdAndUserId(sessionId, userId);
    }

    @Test
    void execute_WhenTutorSessionDoesNotExist_ShouldThrowException() {
        UUID userId = UUID.randomUUID();
        Tutor tutor = tutor(UUID.randomUUID(), Set.of());
        givenAuthenticatedTutor(userId, tutor);
        when(chatSessionRepository.findOpenByIdAndUserId(sessionId, userId))
                .thenReturn(Optional.empty());

        Executable execution = () -> whenTutorAsks(sessionId, "hijos");

        assertThrows(ChatSessionNotFoundException.class, execution);
    }

    @Test
    void execute_WhenTutorHasIncompleteChildren_ShouldBuildReadableFacts() {
        UUID userId = UUID.randomUUID();
        UUID tutorId = UUID.randomUUID();
        Alumno incompleteChild = new Alumno();
        incompleteChild.setId(UUID.randomUUID());
        incompleteChild.setRestriccionesSalud(null);
        incompleteChild.setProductosBloqueados(null);
        Alumno fullChild = fullChild();
        Tutor tutor = tutor(tutorId, Set.of(incompleteChild, fullChild));

        givenAuthenticatedTutor(userId, tutor);
        givenNewUserSession(userId);
        givenRecentMessages(List.of());
        when(presupuestoRepository.findActivoByAlumnoId(incompleteChild.getId()))
                .thenReturn(Optional.empty());
        when(presupuestoRepository.findActivoByAlumnoId(fullChild.getId()))
                .thenReturn(Optional.empty());
        when(purchaseHistoryRepository.findHistorialByTutor(tutorId)).thenReturn(List.of());
        when(paymentRepository.findRecentByStudentId(incompleteChild.getId(), 2))
                .thenReturn(List.of());
        when(paymentRepository.findRecentByStudentId(fullChild.getId(), 2)).thenReturn(List.of());
        givenNoEventsFor(incompleteChild.getId());
        givenNoEventsFor(fullChild.getId());
        givenAiAnswer("Datos de hijos.", "gemini-test");

        whenTutorAsks("datos de mis hijos");

        List<String> facts = thenAiPromptWasSent().context().facts();
        assertTrue(facts.stream().anyMatch(fact ->
                fact.contains("hijo=sin_datos") && fact.contains("restriccionesSalud=[]")));
        assertTrue(facts.stream().anyMatch(fact ->
                fact.contains("hijo=Luz Gomez") && fact.contains("grado=3ro A")
                        && fact.contains("Sin lactosa") && fact.contains("Gaseosa")));
    }

    @Test
    void execute_WhenKiosqueroAsks_ShouldBuildBuffetContext() {
        UUID userId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        Producto product = product();
        Purchase pending = pendingPurchase();

        givenAuthenticatedKiosquero(userId, buffetId);
        givenNewUserSession(userId);
        givenRecentMessages(List.of());
        when(productRepository.findAllActiveWithStock(buffetId)).thenReturn(List.of(product));
        when(purchaseHistoryRepository.findByBuffetAndStatuses(
                eq(buffetId),
                any(),
                eq(10)
        )).thenReturn(List.of(pending));
        when(purchaseHistoryRepository.findRecentByBuffet(buffetId, 10))
                .thenReturn(List.of(pending));
        givenAiAnswer("Tenes 7 sandwiches en stock.", "gemini-test");

        AskSchoolAssistantResponse response = whenKiosqueroAsks("stock");

        assertEquals("Tenes 7 sandwiches en stock.", response.answer());
        assertTrue(response.capabilities().contains("STOCK"));
        assertTrue(response.capabilities().contains("PEDIDOS"));

        SchoolAssistantPrompt prompt = thenAiPromptWasSent();
        assertEquals("KIOSQUERO", prompt.context().role().name());
        assertEquals(buffetId, prompt.context().buffetId());
        assertTrue(prompt.context().facts().stream().anyMatch(fact ->
                fact.contains("productoBuffet=") && fact.contains("Sandwich")));
    }

    @Test
    void execute_WhenKiosqueroAiDoesNotAnswer_ShouldUseKiosqueroFallback() {
        UUID userId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        givenAuthenticatedKiosquero(userId, buffetId);
        givenNewUserSession(userId);
        givenRecentMessages(List.of());
        when(productRepository.findAllActiveWithStock(buffetId)).thenReturn(List.of());
        when(purchaseHistoryRepository.findByBuffetAndStatuses(
                eq(buffetId),
                any(),
                eq(10)
        )).thenReturn(List.of());
        when(purchaseHistoryRepository.findRecentByBuffet(buffetId, 10)).thenReturn(List.of());
        givenNoAiAnswer();

        AskSchoolAssistantResponse response = whenKiosqueroAsks("otra consulta");

        assertEquals(
                "No pude responder ahora. Puedo ayudarte con stock, ventas y pedidos del buffet.",
                response.answer()
        );
        assertEquals("KIOSQUERO", response.role());
        assertTrue(response.suggestions().stream().anyMatch(suggestion ->
                "CONSULTA_STOCK".equals(suggestion.actionType())));
    }

    @Test
    void execute_WhenAuthenticatedUserDoesNotExist_ShouldThrowException() {
        when(usuarioRepository.findByIdCognito(TUTOR_SUBJECT)).thenReturn(Optional.empty());

        Executable execution = () -> whenTutorAsks("hijos");

        assertThrows(UsuarioAutenticadoNoEncontradoException.class, execution);
    }

    @Test
    void execute_WhenTutorDoesNotExist_ShouldThrowException() {
        UUID userId = UUID.randomUUID();
        when(usuarioRepository.findByIdCognito(TUTOR_SUBJECT))
                .thenReturn(Optional.of(user(userId)));
        when(tutorRepository.findByUsuarioIdCognito(TUTOR_SUBJECT)).thenReturn(Optional.empty());

        Executable execution = () -> whenTutorAsks("hijos");

        assertThrows(TutorNoEncontradoException.class, execution);
    }

    @Test
    void execute_WhenKiosqueroHasNoBuffet_ShouldThrowException() {
        UUID userId = UUID.randomUUID();
        when(usuarioRepository.findByIdCognito(KIOSQUERO_SUBJECT))
                .thenReturn(Optional.of(user(userId)));
        when(kiosqueroRepository.findBuffetByUsuarioId(userId)).thenReturn(null);

        Executable execution = () -> whenKiosqueroAsks("stock");

        assertThrows(KiosqueroNotFoundException.class, execution);
    }

    @Test
    void execute_WhenFixedReplyHasAction_ShouldStoreActionMetadata() {
        UUID purchaseId = UUID.randomUUID();
        SchoolAssistantAction action = new SchoolAssistantAction(
                SchoolAssistantActionType.COMPRA_ANTICIPADA,
                SchoolAssistantActionStatus.EJECUTADA,
                false,
                List.of(SchoolAssistantRequiredInput.RECREO),
                purchaseId,
                EstadoCompra.PENDIENTE,
                "ABC123",
                new BigDecimal("900.00"),
                RecessTime.FIRST_RECESS,
                Map.of("producto", "Yogur")
        );
        SchoolAssistantSuggestion suggestion = new SchoolAssistantSuggestion(
                "Saldo",
                "cuanto saldo tengo",
                SchoolAssistantActionType.CONSULTA_SALDO
        );
        SchoolAssistantFixedReply fixedReply = new SchoolAssistantFixedReply(
                "Compra creada.",
                Map.of("intencionChatbot", "CONFIRM_PURCHASE"),
                SchoolAssistantIntent.CONFIRM_PURCHASE,
                action,
                List.of(suggestion),
                Map.of("compraId", purchaseId.toString())
        );

        givenAuthenticatedStudent(studentId);
        givenNewStudentSession();
        givenRecentMessages(List.of());
        givenEmptyStudentContext();
        givenFixedReply("confirmar", fixedReply);

        AskSchoolAssistantResponse response = whenStudentAsks("confirmar");

        assertEquals("COMPRA_ANTICIPADA", response.action().type());
        assertEquals("primer recreo", response.action().recessTime());
        Map<?, ?> metadata = storedAssistantMetadata();
        Map<?, ?> actionMetadata = (Map<?, ?>) metadata.get("accion");
        assertEquals("COMPRA_ANTICIPADA", actionMetadata.get("tipo"));
        assertEquals("EJECUTADA", actionMetadata.get("estado"));
        assertEquals(purchaseId.toString(), actionMetadata.get("compraId"));
        assertEquals("ABC123", actionMetadata.get("codigoRetiro"));
        assertEquals("FIRST_RECESS", actionMetadata.get("recreo"));
        assertEquals("CONFIRM_PURCHASE", metadata.get("intencionChatbot"));
    }

    @Test
    void execute_WhenRequestIsNull_ShouldThrowException() {
        Executable execution = () -> askSchoolAssistant.execute(null);

        assertThrows(IllegalArgumentException.class, execution);
    }

    @Test
    void execute_WhenRoleIsNull_ShouldThrowException() {
        AskSchoolAssistantRequest request = new AskSchoolAssistantRequest(
                STUDENT_SUBJECT,
                null,
                null,
                "saldo"
        );

        Executable execution = () -> askSchoolAssistant.execute(request);

        assertThrows(IllegalArgumentException.class, execution);
    }

    private void givenAuthenticatedStudent(UUID studentId) {
        givenAuthenticatedUser(STUDENT_SUBJECT, studentId);
        when(alumnoRepository.existsById(studentId)).thenReturn(true);
    }

    private void givenAuthenticatedUser(String cognitoSubject, UUID userId) {
        when(usuarioRepository.findByIdCognito(cognitoSubject))
                .thenReturn(Optional.of(user(userId)));
    }

    private void givenStudentResolvedFromUser(UUID userId) {
        when(alumnoRepository.existsById(userId)).thenReturn(false);
        when(alumnoRepository.findStudentIdByUserId(userId)).thenReturn(Optional.of(studentId));
    }

    private void givenAuthenticatedTutor(UUID userId, Tutor tutor) {
        givenAuthenticatedUser(TUTOR_SUBJECT, userId);
        when(tutorRepository.findByUsuarioIdCognito(TUTOR_SUBJECT)).thenReturn(Optional.of(tutor));
    }

    private void givenAuthenticatedKiosquero(UUID userId, UUID buffetId) {
        givenAuthenticatedUser(KIOSQUERO_SUBJECT, userId);
        when(kiosqueroRepository.findBuffetByUsuarioId(userId)).thenReturn(buffetId);
    }

    private void givenNewStudentSession() {
        when(chatSessionRepository.startForStudent(studentId, NOW)).thenReturn(session);
    }

    private void givenNewUserSession(UUID userId) {
        when(chatSessionRepository.startForUser(userId, NOW)).thenReturn(session);
    }

    private void givenRecentMessages(List<ChatMessage> messages) {
        when(chatMessageRepository.findRecentBySessionId(sessionId, 6)).thenReturn(messages);
    }

    private void givenEmptyStudentContext() {
        givenStudentContext(Optional.empty(), List.of(), List.of(), List.of());
    }

    private void givenStudentContext(
            Optional<Billetera> wallet,
            List<SchoolAssistantPayment> payments,
            List<SchoolAssistantPurchase> purchases,
            List<SchoolCalendarEvent> events
    ) {
        when(walletRepository.findByStudentId(studentId)).thenReturn(wallet);
        when(paymentRepository.findRecentByStudentId(studentId, 5)).thenReturn(payments);
        when(purchaseRepository.findRecentByStudentId(studentId, 10)).thenReturn(purchases);
        when(schoolCalendarRepository.findByStudentIdAndDateRange(
                studentId,
                LocalDate.of(2026, 5, 27),
                LocalDate.of(2026, 6, 26)
        )).thenReturn(events);
    }

    private void givenNoEventsFor(UUID studentId) {
        when(schoolCalendarRepository.findByStudentIdAndDateRange(
                eq(studentId),
                eq(LocalDate.of(2026, 5, 27)),
                eq(LocalDate.of(2026, 6, 26))
        )).thenReturn(List.of());
    }

    private void givenAiAnswer(String answer, String model) {
        when(schoolAssistantResponder.respond(any())).thenReturn(Optional.of(
                new SchoolAssistantDraft(answer, model)
        ));
    }

    private void givenNoAiAnswer() {
        when(schoolAssistantResponder.respond(any())).thenReturn(Optional.empty());
    }

    private void givenFixedReply(String message, SchoolAssistantFixedReply fixedReply) {
        when(fixedTopics.respond(eq(message), any(), any())).thenReturn(Optional.of(fixedReply));
    }

    private AskSchoolAssistantResponse whenStudentAsks(String message) {
        return whenStudentAsks(null, message);
    }

    private AskSchoolAssistantResponse whenStudentAsks(UUID sessionId, String message) {
        return askSchoolAssistant.execute(new AskSchoolAssistantRequest(
                STUDENT_SUBJECT,
                SchoolAssistantRole.ALUMNO,
                sessionId,
                message
        ));
    }

    private AskSchoolAssistantResponse whenTutorAsks(String message) {
        return whenTutorAsks(null, message);
    }

    private AskSchoolAssistantResponse whenTutorAsks(UUID sessionId, String message) {
        return askSchoolAssistant.execute(new AskSchoolAssistantRequest(
                TUTOR_SUBJECT,
                SchoolAssistantRole.TUTOR,
                sessionId,
                message
        ));
    }

    private AskSchoolAssistantResponse whenKiosqueroAsks(String message) {
        return askSchoolAssistant.execute(new AskSchoolAssistantRequest(
                KIOSQUERO_SUBJECT,
                SchoolAssistantRole.KIOSQUERO,
                null,
                message
        ));
    }

    private SchoolAssistantPrompt thenAiPromptWasSent() {
        ArgumentCaptor<SchoolAssistantPrompt> promptCaptor = ArgumentCaptor
                .forClass(SchoolAssistantPrompt.class);
        verify(schoolAssistantResponder).respond(promptCaptor.capture());
        return promptCaptor.getValue();
    }

    private void thenMessagesWereStored(String assistantContent, boolean generatedByAi) {
        Map<?, ?> metadata = storedAssistantMetadata();
        ArgumentCaptor<ChatMessage> messageCaptor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatMessageRepository, times(2)).save(messageCaptor.capture());
        List<ChatMessage> messages = messageCaptor.getAllValues();
        assertEquals(RolChatMensaje.USUARIO, messages.get(0).role());
        assertEquals(RolChatMensaje.ASISTENTE_IA, messages.get(1).role());
        assertEquals(assistantContent, messages.get(1).content());
        assertEquals(generatedByAi, metadata.get("generadoPorIa"));
        verify(chatSessionRepository).updateLastActivity(sessionId, NOW);
    }

    private Map<?, ?> storedAssistantMetadata() {
        ArgumentCaptor<ChatMessage> messageCaptor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatMessageRepository, times(2)).save(messageCaptor.capture());
        return messageCaptor.getAllValues().get(1).metadata();
    }

    private Billetera wallet(BigDecimal balance) {
        Billetera wallet = new Billetera();
        wallet.setSaldoCreditos(balance);
        return wallet;
    }

    private SchoolAssistantPayment payment() {
        return new SchoolAssistantPayment(
                new BigDecimal("500.00"),
                EstadoPagoExterno.APROBADO,
                NOW.minusDays(1),
                NOW.minusDays(1)
        );
    }

    private SchoolAssistantPurchase purchase() {
        return new SchoolAssistantPurchase(
                new BigDecimal("320.00"),
                EstadoCompra.ENTREGADO,
                MedioPago.CREDITOS,
                NOW.minusHours(2)
        );
    }

    private SchoolCalendarEvent event() {
        return new SchoolCalendarEvent(
                LocalDate.of(2026, 5, 29),
                TipoDiaEscolar.JORNADA_ESPECIAL,
                "Acto escolar"
        );
    }

    private Usuario user(UUID userId) {
        Usuario user = new Usuario();
        user.setId(userId);
        return user;
    }

    private Tutor tutor(UUID tutorId, Set<Alumno> children) {
        Tutor tutor = new Tutor();
        tutor.setId(tutorId);
        tutor.setHijos(children);
        return tutor;
    }

    private Alumno child(String firstName, String lastName, BigDecimal balance) {
        Usuario childUser = new Usuario();
        childUser.setNombre(firstName);
        childUser.setApellido(lastName);
        Alumno child = new Alumno();
        child.setId(UUID.randomUUID());
        child.setUsuario(childUser);
        child.setBilletera(wallet(balance));
        return child;
    }

    private Alumno fullChild() {
        Alumno child = child("Luz", "Gomez", new BigDecimal("1000.00"));
        Grado grade = new Grado();
        grade.setAnio("3ro");
        grade.setDivision("A");
        ClasificacionSalud restriction = new ClasificacionSalud();
        restriction.setDescripcion("Sin lactosa");
        Producto blockedProduct = new Producto();
        blockedProduct.setNombre("Gaseosa");
        child.setGrado(grade);
        child.setRestriccionesSalud(Set.of(restriction));
        child.setProductosBloqueados(Set.of(blockedProduct));
        return child;
    }

    private Purchase tutorPurchase(Alumno child) {
        Purchase purchase = new Purchase();
        purchase.setId(UUID.randomUUID());
        purchase.setStudent(child);
        purchase.setTotalAmount(new BigDecimal("350.00"));
        purchase.setStatus(EstadoCompra.ENTREGADO);
        purchase.setPaymentMethod(MedioPago.CREDITOS);
        purchase.setDate(NOW.minusHours(1));
        return purchase;
    }

    private Producto product() {
        Producto product = new Producto();
        product.setId(UUID.randomUUID());
        product.setNombre("Sandwich");
        product.setPrecio(new BigDecimal("1200.00"));
        product.setStockActual(7);
        return product;
    }

    private Purchase pendingPurchase() {
        Purchase purchase = new Purchase();
        purchase.setId(UUID.randomUUID());
        purchase.setTotalAmount(new BigDecimal("1200.00"));
        purchase.setStatus(EstadoCompra.PENDIENTE);
        purchase.setPaymentMethod(MedioPago.CREDITOS);
        purchase.setDate(NOW.minusMinutes(15));
        return purchase;
    }
}
