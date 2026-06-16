package com.recred.backend.unit.application;

import com.recred.backend.application.AskSchoolAssistant;
import com.recred.backend.application.SchoolAssistantCommand;
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
import com.recred.backend.domain.models.SchoolAssistantAnswer;
import com.recred.backend.domain.models.SchoolAssistantCapability;
import com.recred.backend.domain.models.SchoolAssistantDraft;
import com.recred.backend.domain.models.SchoolAssistantPayment;
import com.recred.backend.domain.models.SchoolAssistantPrompt;
import com.recred.backend.domain.models.SchoolAssistantPurchase;
import com.recred.backend.domain.models.SchoolAssistantRequiredInput;
import com.recred.backend.domain.models.SchoolAssistantIntent;
import com.recred.backend.domain.models.SchoolCalendarEvent;
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
    private static final List<SchoolAssistantCapability> ALLOWED_CAPABILITIES = List.of(
            SchoolAssistantCapability.SALDO,
            SchoolAssistantCapability.PAGOS,
            SchoolAssistantCapability.COMPRAS,
            SchoolAssistantCapability.EVENTOS
    );

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
    void executeBuildsFullContextAndReturnsAiAnswer() {
        Billetera wallet = wallet(new BigDecimal("1500.00"));
        SchoolAssistantPayment payment = new SchoolAssistantPayment(
                new BigDecimal("500.00"),
                EstadoPagoExterno.APROBADO,
                NOW.minusDays(1),
                NOW.minusDays(1)
        );
        SchoolAssistantPurchase purchase = new SchoolAssistantPurchase(
                new BigDecimal("320.00"),
                EstadoCompra.ENTREGADO,
                MedioPago.CREDITOS,
                NOW.minusHours(2)
        );
        SchoolCalendarEvent event = new SchoolCalendarEvent(
                LocalDate.of(2026, 5, 29),
                TipoDiaEscolar.JORNADA_ESPECIAL,
                "Acto escolar"
        );

        when(alumnoRepository.existsById(studentId)).thenReturn(true);
        when(chatSessionRepository.startForStudent(studentId, NOW)).thenReturn(session);
        when(chatMessageRepository.findRecentBySessionId(sessionId, 6)).thenReturn(List.of());
        when(walletRepository.findByStudentId(studentId)).thenReturn(Optional.of(wallet));
        when(paymentRepository.findRecentByStudentId(studentId, 5)).thenReturn(List.of(payment));
        when(purchaseRepository.findRecentByStudentId(studentId, 10)).thenReturn(List.of(purchase));
        when(schoolCalendarRepository.findByStudentIdAndDateRange(
                studentId,
                LocalDate.of(2026, 5, 27),
                LocalDate.of(2026, 6, 26)
        )).thenReturn(List.of(event));
        when(schoolAssistantResponder.respond(any())).thenReturn(Optional.of(
                new SchoolAssistantDraft("Respuesta IA.", "gemini-test")
        ));

        SchoolAssistantAnswer answer = askSchoolAssistant.execute(
                studentId,
                new SchoolAssistantCommand(
                        Optional.empty(),
                        "Quiero ver saldo, pagos, compras y eventos"
                )
        );

        assertEquals(sessionId, answer.sessionId());
        assertEquals("Respuesta IA.", answer.answer());
        assertTrue(answer.generatedByAi());
        assertEquals("gemini-test", answer.model());
        assertEquals(ALLOWED_CAPABILITIES, answer.capabilities());
        assertEquals(SchoolAssistantRole.ALUMNO, answer.role());
        assertEquals(SchoolAssistantIntent.UNKNOWN, answer.intent());
        assertFalse(answer.suggestions().isEmpty());

        ArgumentCaptor<SchoolAssistantPrompt> promptCaptor = ArgumentCaptor
                .forClass(SchoolAssistantPrompt.class);
        verify(schoolAssistantResponder).respond(promptCaptor.capture());
        SchoolAssistantPrompt prompt = promptCaptor.getValue();
        assertEquals("Quiero ver saldo, pagos, compras y eventos", prompt.question());
        assertEquals(studentId, prompt.context().studentId());
        assertEquals(new BigDecimal("1500.00"), prompt.context().balance().orElseThrow());
        assertEquals(payment, prompt.context().recentPayments().get(0));
        assertEquals(purchase, prompt.context().recentPurchases().get(0));
        assertEquals(event, prompt.context().upcomingEvents().get(0));
        assertTrue(prompt.recentMessages().isEmpty());

        ArgumentCaptor<ChatMessage> messageCaptor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatMessageRepository, times(2)).save(messageCaptor.capture());
        List<ChatMessage> messages = messageCaptor.getAllValues();
        assertEquals(RolChatMensaje.USUARIO, messages.get(0).role());
        assertEquals(RolChatMensaje.ASISTENTE_IA, messages.get(1).role());
        assertEquals("Respuesta IA.", messages.get(1).content());
        assertEquals(true, messages.get(1).metadata().get("generadoPorIa"));
        assertEquals("gemini-test", messages.get(1).metadata().get("modelo"));
        assertEquals(
                List.of("SALDO", "PAGOS", "COMPRAS", "EVENTOS"),
                messages.get(0).metadata().get("capacidadesPermitidas")
        );
        verify(chatSessionRepository).updateLastActivity(sessionId, NOW);
    }

    @Test
    void executeFormatsAiAnswerBeforeReturningAndSaving() {
        String rawAnswer = "Stock actual: Coca-Cola 500ml por $1200.00 (stock: 50); "
                + "Jugo de Naranja 300ml por $950.00 (stock: 35); "
                + "Papas Chips 50g por $600.00 (stock: 70).";
        String expectedAnswer = """
                Stock actual:
                - Coca-Cola 500ml por $1200.00 (stock: 50)
                - Jugo de Naranja 300ml por $950.00 (stock: 35)
                - Papas Chips 50g por $600.00 (stock: 70)
                """.trim();

        when(alumnoRepository.existsById(studentId)).thenReturn(true);
        when(chatSessionRepository.startForStudent(studentId, NOW)).thenReturn(session);
        when(chatMessageRepository.findRecentBySessionId(sessionId, 6)).thenReturn(List.of());
        when(walletRepository.findByStudentId(studentId)).thenReturn(Optional.empty());
        when(paymentRepository.findRecentByStudentId(studentId, 5)).thenReturn(List.of());
        when(purchaseRepository.findRecentByStudentId(studentId, 10)).thenReturn(List.of());
        when(schoolCalendarRepository.findByStudentIdAndDateRange(
                studentId,
                LocalDate.of(2026, 5, 27),
                LocalDate.of(2026, 6, 26)
        )).thenReturn(List.of());
        when(schoolAssistantResponder.respond(any())).thenReturn(Optional.of(
                new SchoolAssistantDraft(rawAnswer, "gemini-test")
        ));

        SchoolAssistantAnswer answer = askSchoolAssistant.execute(
                studentId,
                new SchoolAssistantCommand(Optional.empty(), "stock")
        );

        assertEquals(expectedAnswer, answer.answer());
        assertFalse(answer.answer().contains(";"));

        ArgumentCaptor<ChatMessage> messageCaptor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatMessageRepository, times(2)).save(messageCaptor.capture());
        ChatMessage assistantMessage = messageCaptor.getAllValues().get(1);
        assertEquals(expectedAnswer, assistantMessage.content());
    }

    @Test
    void executeResolvesStudentIdFromUserId() {
        UUID userId = UUID.randomUUID();
        Billetera wallet = wallet(new BigDecimal("1500.00"));

        when(alumnoRepository.existsById(userId)).thenReturn(false);
        when(alumnoRepository.findStudentIdByUserId(userId)).thenReturn(Optional.of(studentId));
        when(chatSessionRepository.startForStudent(studentId, NOW)).thenReturn(session);
        when(chatMessageRepository.findRecentBySessionId(sessionId, 6)).thenReturn(List.of());
        when(walletRepository.findByStudentId(studentId)).thenReturn(Optional.of(wallet));
        when(paymentRepository.findRecentByStudentId(studentId, 5)).thenReturn(List.of());
        when(purchaseRepository.findRecentByStudentId(studentId, 10)).thenReturn(List.of());
        when(schoolCalendarRepository.findByStudentIdAndDateRange(
                studentId,
                LocalDate.of(2026, 5, 27),
                LocalDate.of(2026, 6, 26)
        )).thenReturn(List.of());
        when(schoolAssistantResponder.respond(any())).thenReturn(Optional.empty());

        SchoolAssistantAnswer answer = askSchoolAssistant.execute(
                userId,
                new SchoolAssistantCommand(Optional.empty(), "saldo")
        );

        assertEquals(sessionId, answer.sessionId());
        verify(chatSessionRepository).startForStudent(studentId, NOW);
        verify(walletRepository).findByStudentId(studentId);
    }

    @Test
    void executeUsesFallbackWhenGeminiDoesNotReturnAnswer() {
        when(alumnoRepository.existsById(studentId)).thenReturn(true);
        when(chatSessionRepository.startForStudent(studentId, NOW)).thenReturn(session);
        when(chatMessageRepository.findRecentBySessionId(sessionId, 6)).thenReturn(List.of());
        when(walletRepository.findByStudentId(studentId)).thenReturn(Optional.empty());
        when(paymentRepository.findRecentByStudentId(studentId, 5)).thenReturn(List.of());
        when(purchaseRepository.findRecentByStudentId(studentId, 10)).thenReturn(List.of());
        when(schoolCalendarRepository.findByStudentIdAndDateRange(
                studentId,
                LocalDate.of(2026, 5, 27),
                LocalDate.of(2026, 6, 26)
        )).thenReturn(List.of());
        when(schoolAssistantResponder.respond(any())).thenReturn(Optional.empty());

        SchoolAssistantAnswer answer = askSchoolAssistant.execute(
                studentId,
                new SchoolAssistantCommand(Optional.empty(), "cuanto saldo tengo")
        );

        assertEquals(
                "No pude responder ahora. Puedo ayudarte con saldos, pagos, compras "
                        + "y eventos escolares.",
                answer.answer()
        );
        assertFalse(answer.generatedByAi());
        assertNull(answer.model());
        assertEquals(ALLOWED_CAPABILITIES, answer.capabilities());
        assertFalse(answer.suggestions().isEmpty());
        verify(schoolAssistantResponder).respond(any());
    }

    @Test
    void executeUsesFallbackWhenAiDraftIsBlank() {
        when(alumnoRepository.existsById(studentId)).thenReturn(true);
        when(chatSessionRepository.startForStudent(studentId, NOW)).thenReturn(session);
        when(chatMessageRepository.findRecentBySessionId(sessionId, 6)).thenReturn(List.of());
        when(walletRepository.findByStudentId(studentId)).thenReturn(Optional.empty());
        when(paymentRepository.findRecentByStudentId(studentId, 5)).thenReturn(List.of());
        when(purchaseRepository.findRecentByStudentId(studentId, 10)).thenReturn(List.of());
        when(schoolCalendarRepository.findByStudentIdAndDateRange(
                studentId,
                LocalDate.of(2026, 5, 27),
                LocalDate.of(2026, 6, 26)
        )).thenReturn(List.of());
        when(schoolAssistantResponder.respond(any())).thenReturn(Optional.of(
                new SchoolAssistantDraft(" ", "gemini-test")
        ));

        SchoolAssistantAnswer answer = askSchoolAssistant.execute(
                studentId,
                new SchoolAssistantCommand(Optional.empty(), "otra consulta")
        );

        assertEquals(
                "No pude responder ahora. Puedo ayudarte con saldos, pagos, compras "
                        + "y eventos escolares.",
                answer.answer()
        );
        assertFalse(answer.generatedByAi());
        assertNull(answer.model());
    }

    @Test
    void executeUsesFixedReplyBeforeGemini() {
        Map<String, Object> metadata = Map.of("intencionChatbot", "BALANCE");

        when(alumnoRepository.existsById(studentId)).thenReturn(true);
        when(chatSessionRepository.startForStudent(studentId, NOW)).thenReturn(session);
        when(chatMessageRepository.findRecentBySessionId(sessionId, 6)).thenReturn(List.of());
        when(walletRepository.findByStudentId(studentId))
                .thenReturn(Optional.of(wallet(new BigDecimal("1500.00"))));
        when(paymentRepository.findRecentByStudentId(studentId, 5)).thenReturn(List.of());
        when(purchaseRepository.findRecentByStudentId(studentId, 10)).thenReturn(List.of());
        when(schoolCalendarRepository.findByStudentIdAndDateRange(
                studentId,
                LocalDate.of(2026, 5, 27),
                LocalDate.of(2026, 6, 26)
        )).thenReturn(List.of());
        when(fixedTopics.respond(eq("saldo"), any(), any())).thenReturn(Optional.of(
                new SchoolAssistantFixedReply("Tenes $1500.00 disponibles.", metadata)
        ));

        SchoolAssistantAnswer answer = askSchoolAssistant.execute(
                studentId,
                new SchoolAssistantCommand(Optional.empty(), "saldo")
        );

        assertEquals("Tenes $1500.00 disponibles.", answer.answer());
        assertFalse(answer.generatedByAi());
        assertNull(answer.model());
        verify(schoolAssistantResponder, never()).respond(any());

        ArgumentCaptor<ChatMessage> messageCaptor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatMessageRepository, times(2)).save(messageCaptor.capture());
        ChatMessage assistantMessage = messageCaptor.getAllValues().get(1);
        assertEquals("BALANCE", assistantMessage.metadata().get("intencionChatbot"));
        assertEquals(false, assistantMessage.metadata().get("generadoPorIa"));
    }

    @Test
    void executeThrowsWhenMessageIsBlank() {
        when(alumnoRepository.existsById(studentId)).thenReturn(true);

        assertThrows(
                InvalidChatMessageException.class,
                () -> askSchoolAssistant.execute(
                        studentId,
                        new SchoolAssistantCommand(Optional.empty(), " ")
                )
        );

        verify(chatSessionRepository, never()).startForStudent(any(), any());
    }

    @Test
    void executeThrowsWhenMessageIsNull() {
        when(alumnoRepository.existsById(studentId)).thenReturn(true);

        assertThrows(
                InvalidChatMessageException.class,
                () -> askSchoolAssistant.execute(
                        studentId,
                        new SchoolAssistantCommand(Optional.empty(), null)
                )
        );

        verify(chatSessionRepository, never()).startForStudent(any(), any());
    }

    @Test
    void executeThrowsWhenStudentCannotBeResolvedFromUserId() {
        UUID userId = UUID.randomUUID();
        when(alumnoRepository.existsById(userId)).thenReturn(false);
        when(alumnoRepository.findStudentIdByUserId(userId)).thenReturn(Optional.empty());

        assertThrows(
                AlumnoNoEncontradoException.class,
                () -> askSchoolAssistant.execute(
                        userId,
                        new SchoolAssistantCommand(Optional.empty(), "saldo")
                )
        );
    }

    @Test
    void executeThrowsWhenRequestedSessionIsNotOpenForStudent() {
        when(alumnoRepository.existsById(studentId)).thenReturn(true);
        when(chatSessionRepository.findOpenByIdAndStudentId(sessionId, studentId))
                .thenReturn(Optional.empty());

        assertThrows(
                ChatSessionNotFoundException.class,
                () -> askSchoolAssistant.execute(
                        studentId,
                        new SchoolAssistantCommand(Optional.of(sessionId), "saldo")
                )
        );

        verify(chatMessageRepository, never()).save(any());
    }

    @Test
    void executeUsesExistingStudentSessionWhenProvided() {
        when(alumnoRepository.existsById(studentId)).thenReturn(true);
        when(chatSessionRepository.findOpenByIdAndStudentId(sessionId, studentId))
                .thenReturn(Optional.of(session));
        when(chatMessageRepository.findRecentBySessionId(sessionId, 6)).thenReturn(List.of());
        when(walletRepository.findByStudentId(studentId)).thenReturn(Optional.empty());
        when(paymentRepository.findRecentByStudentId(studentId, 5)).thenReturn(List.of());
        when(purchaseRepository.findRecentByStudentId(studentId, 10)).thenReturn(List.of());
        when(schoolCalendarRepository.findByStudentIdAndDateRange(
                studentId,
                LocalDate.of(2026, 5, 27),
                LocalDate.of(2026, 6, 26)
        )).thenReturn(List.of());
        when(schoolAssistantResponder.respond(any())).thenReturn(Optional.empty());

        SchoolAssistantAnswer answer = askSchoolAssistant.execute(
                studentId,
                new SchoolAssistantCommand(Optional.of(sessionId), "saldo")
        );

        assertEquals(sessionId, answer.sessionId());
        verify(chatSessionRepository, never()).startForStudent(any(), any());
        verify(chatSessionRepository).findOpenByIdAndStudentId(sessionId, studentId);
    }

    @Test
    void executeForTutorBuildsContextFromChildren() {
        String cognitoSubject = "tutor-sub";
        UUID userId = UUID.randomUUID();
        UUID tutorId = UUID.randomUUID();
        UUID childId = UUID.randomUUID();
        Usuario user = new Usuario();
        user.setId(userId);
        Usuario childUser = new Usuario();
        childUser.setNombre("Mateo");
        childUser.setApellido("Perez");
        Alumno child = new Alumno();
        child.setId(childId);
        child.setUsuario(childUser);
        child.setBilletera(wallet(new BigDecimal("900.00")));
        Tutor tutor = new Tutor();
        tutor.setId(tutorId);
        tutor.setHijos(Set.of(child));
        PresupuestoAlumno budget = new PresupuestoAlumno();
        budget.setMontoLimiteGeneral(new BigDecimal("2000.00"));
        Purchase purchase = new Purchase();
        purchase.setId(UUID.randomUUID());
        purchase.setStudent(child);
        purchase.setTotalAmount(new BigDecimal("350.00"));
        purchase.setStatus(EstadoCompra.ENTREGADO);
        purchase.setPaymentMethod(MedioPago.CREDITOS);
        purchase.setDate(NOW.minusHours(1));

        when(usuarioRepository.findByIdCognito(cognitoSubject)).thenReturn(Optional.of(user));
        when(tutorRepository.findByUsuarioIdCognito(cognitoSubject)).thenReturn(Optional.of(tutor));
        when(chatSessionRepository.startForUser(userId, NOW)).thenReturn(session);
        when(chatMessageRepository.findRecentBySessionId(sessionId, 6)).thenReturn(List.of());
        when(presupuestoRepository.findActivoByAlumnoId(childId)).thenReturn(Optional.of(budget));
        when(purchaseHistoryRepository.findHistorialByTutor(tutorId)).thenReturn(List.of(purchase));
        when(paymentRepository.findRecentByStudentId(childId, 2)).thenReturn(List.of());
        when(schoolCalendarRepository.findByStudentIdAndDateRange(
                childId,
                LocalDate.of(2026, 5, 27),
                LocalDate.of(2026, 6, 26)
        )).thenReturn(List.of());
        when(schoolAssistantResponder.respond(any())).thenReturn(Optional.of(
                new SchoolAssistantDraft("Mateo tiene $900.00.", "gemini-test")
        ));

        SchoolAssistantAnswer answer = askSchoolAssistant.executeForTutor(
                cognitoSubject,
                new SchoolAssistantCommand(Optional.empty(), "saldo de mis hijos")
        );

        assertEquals("Mateo tiene $900.00.", answer.answer());
        assertTrue(answer.capabilities().contains(SchoolAssistantCapability.HIJOS));
        assertTrue(answer.capabilities().contains(SchoolAssistantCapability.PRESUPUESTOS));
        verify(chatSessionRepository).startForUser(userId, NOW);

        ArgumentCaptor<SchoolAssistantPrompt> promptCaptor = ArgumentCaptor
                .forClass(SchoolAssistantPrompt.class);
        verify(schoolAssistantResponder).respond(promptCaptor.capture());
        SchoolAssistantPrompt prompt = promptCaptor.getValue();
        assertEquals("TUTOR", prompt.context().role().name());
        assertTrue(prompt.context().facts().stream().anyMatch(fact ->
                fact.contains("hijo=Mateo Perez") && fact.contains("saldo=900.00")));
    }

    @Test
    void executeForTutorUsesExistingSessionAndFallback() {
        String cognitoSubject = "tutor-sub";
        UUID userId = UUID.randomUUID();
        UUID tutorId = UUID.randomUUID();
        Usuario user = user(userId);
        Tutor tutor = new Tutor();
        tutor.setId(tutorId);
        tutor.setHijos(Set.of());

        when(usuarioRepository.findByIdCognito(cognitoSubject)).thenReturn(Optional.of(user));
        when(tutorRepository.findByUsuarioIdCognito(cognitoSubject)).thenReturn(Optional.of(tutor));
        when(chatSessionRepository.findOpenByIdAndUserId(sessionId, userId))
                .thenReturn(Optional.of(session));
        when(chatMessageRepository.findRecentBySessionId(sessionId, 6)).thenReturn(List.of());
        when(purchaseHistoryRepository.findHistorialByTutor(tutorId)).thenReturn(List.of());
        when(schoolAssistantResponder.respond(any())).thenReturn(Optional.empty());

        SchoolAssistantAnswer answer = askSchoolAssistant.executeForTutor(
                cognitoSubject,
                new SchoolAssistantCommand(Optional.of(sessionId), "otra consulta")
        );

        assertEquals(
                "No pude responder ahora. Puedo ayudarte con saldos, pagos, compras, "
                        + "eventos, presupuestos y restricciones de tus hijos.",
                answer.answer()
        );
        assertEquals(SchoolAssistantRole.TUTOR, answer.role());
        verify(chatSessionRepository).findOpenByIdAndUserId(sessionId, userId);
    }

    @Test
    void executeForTutorThrowsWhenRequestedSessionIsNotOpenForUser() {
        String cognitoSubject = "tutor-sub";
        UUID userId = UUID.randomUUID();
        Usuario user = user(userId);
        Tutor tutor = new Tutor();
        tutor.setId(UUID.randomUUID());

        when(usuarioRepository.findByIdCognito(cognitoSubject)).thenReturn(Optional.of(user));
        when(tutorRepository.findByUsuarioIdCognito(cognitoSubject)).thenReturn(Optional.of(tutor));
        when(chatSessionRepository.findOpenByIdAndUserId(sessionId, userId))
                .thenReturn(Optional.empty());

        assertThrows(
                ChatSessionNotFoundException.class,
                () -> askSchoolAssistant.executeForTutor(
                        cognitoSubject,
                        new SchoolAssistantCommand(Optional.of(sessionId), "hijos")
                )
        );
    }

    @Test
    void executeForTutorHandlesNullChildrenSet() {
        String cognitoSubject = "tutor-sub";
        UUID userId = UUID.randomUUID();
        UUID tutorId = UUID.randomUUID();
        Usuario user = user(userId);
        Tutor tutor = new Tutor();
        tutor.setId(tutorId);
        tutor.setHijos(null);

        when(usuarioRepository.findByIdCognito(cognitoSubject)).thenReturn(Optional.of(user));
        when(tutorRepository.findByUsuarioIdCognito(cognitoSubject)).thenReturn(Optional.of(tutor));
        when(chatSessionRepository.startForUser(userId, NOW)).thenReturn(session);
        when(chatMessageRepository.findRecentBySessionId(sessionId, 6)).thenReturn(List.of());
        when(purchaseHistoryRepository.findHistorialByTutor(tutorId)).thenReturn(List.of());
        when(schoolAssistantResponder.respond(any())).thenReturn(Optional.of(
                new SchoolAssistantDraft("Sin hijos asociados.", "gemini-test")
        ));

        SchoolAssistantAnswer answer = askSchoolAssistant.executeForTutor(
                cognitoSubject,
                new SchoolAssistantCommand(Optional.empty(), "hijos")
        );

        assertEquals("Sin hijos asociados.", answer.answer());
    }

    @Test
    void executeForTutorBuildsFactsForIncompleteChildren() {
        String cognitoSubject = "tutor-sub";
        UUID userId = UUID.randomUUID();
        UUID tutorId = UUID.randomUUID();
        UUID incompleteChildId = UUID.randomUUID();
        UUID fullChildId = UUID.randomUUID();
        Usuario user = user(userId);
        Alumno incompleteChild = new Alumno();
        incompleteChild.setId(incompleteChildId);
        incompleteChild.setRestriccionesSalud(null);
        incompleteChild.setProductosBloqueados(null);
        Usuario childUser = new Usuario();
        childUser.setNombre("Luz");
        childUser.setApellido("Gomez");
        Grado grade = new Grado();
        grade.setAnio("3ro");
        grade.setDivision("A");
        ClasificacionSalud restriction = new ClasificacionSalud();
        restriction.setDescripcion("Sin lactosa");
        Producto blockedProduct = new Producto();
        blockedProduct.setNombre("Gaseosa");
        Alumno fullChild = new Alumno();
        fullChild.setId(fullChildId);
        fullChild.setUsuario(childUser);
        fullChild.setGrado(grade);
        fullChild.setRestriccionesSalud(Set.of(restriction));
        fullChild.setProductosBloqueados(Set.of(blockedProduct));
        Tutor tutor = new Tutor();
        tutor.setId(tutorId);
        tutor.setHijos(Set.of(incompleteChild, fullChild));

        when(usuarioRepository.findByIdCognito(cognitoSubject)).thenReturn(Optional.of(user));
        when(tutorRepository.findByUsuarioIdCognito(cognitoSubject)).thenReturn(Optional.of(tutor));
        when(chatSessionRepository.startForUser(userId, NOW)).thenReturn(session);
        when(chatMessageRepository.findRecentBySessionId(sessionId, 6)).thenReturn(List.of());
        when(presupuestoRepository.findActivoByAlumnoId(incompleteChildId))
                .thenReturn(Optional.empty());
        when(presupuestoRepository.findActivoByAlumnoId(fullChildId)).thenReturn(Optional.empty());
        when(purchaseHistoryRepository.findHistorialByTutor(tutorId)).thenReturn(List.of());
        when(paymentRepository.findRecentByStudentId(incompleteChildId, 2)).thenReturn(List.of());
        when(paymentRepository.findRecentByStudentId(fullChildId, 2)).thenReturn(List.of());
        when(schoolCalendarRepository.findByStudentIdAndDateRange(
                eq(incompleteChildId),
                eq(LocalDate.of(2026, 5, 27)),
                eq(LocalDate.of(2026, 6, 26))
        )).thenReturn(List.of());
        when(schoolCalendarRepository.findByStudentIdAndDateRange(
                eq(fullChildId),
                eq(LocalDate.of(2026, 5, 27)),
                eq(LocalDate.of(2026, 6, 26))
        )).thenReturn(List.of());
        when(schoolAssistantResponder.respond(any())).thenReturn(Optional.of(
                new SchoolAssistantDraft("Datos de hijos.", "gemini-test")
        ));

        askSchoolAssistant.executeForTutor(
                cognitoSubject,
                new SchoolAssistantCommand(Optional.empty(), "datos de mis hijos")
        );

        ArgumentCaptor<SchoolAssistantPrompt> promptCaptor = ArgumentCaptor
                .forClass(SchoolAssistantPrompt.class);
        verify(schoolAssistantResponder).respond(promptCaptor.capture());
        List<String> facts = promptCaptor.getValue().context().facts();
        assertTrue(facts.stream().anyMatch(fact ->
                fact.contains("hijo=sin_datos") && fact.contains("restriccionesSalud=[]")));
        assertTrue(facts.stream().anyMatch(fact ->
                fact.contains("hijo=Luz Gomez") && fact.contains("grado=3ro A")
                        && fact.contains("Sin lactosa") && fact.contains("Gaseosa")));
    }

    @Test
    void executeForKiosqueroBuildsContextFromBuffet() {
        String cognitoSubject = "kiosquero-sub";
        UUID userId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        Usuario user = new Usuario();
        user.setId(userId);
        Producto product = new Producto();
        product.setId(UUID.randomUUID());
        product.setNombre("Sandwich");
        product.setPrecio(new BigDecimal("1200.00"));
        product.setStockActual(7);
        Purchase pending = new Purchase();
        pending.setId(UUID.randomUUID());
        pending.setTotalAmount(new BigDecimal("1200.00"));
        pending.setStatus(EstadoCompra.PENDIENTE);
        pending.setPaymentMethod(MedioPago.CREDITOS);
        pending.setDate(NOW.minusMinutes(15));

        when(usuarioRepository.findByIdCognito(cognitoSubject)).thenReturn(Optional.of(user));
        when(kiosqueroRepository.findBuffetByUsuarioId(userId)).thenReturn(buffetId);
        when(chatSessionRepository.startForUser(userId, NOW)).thenReturn(session);
        when(chatMessageRepository.findRecentBySessionId(sessionId, 6)).thenReturn(List.of());
        when(productRepository.findAllActiveWithStock(buffetId)).thenReturn(List.of(product));
        when(purchaseHistoryRepository.findByBuffetAndStatuses(
                eq(buffetId),
                any(),
                eq(10)
        )).thenReturn(List.of(pending));
        when(purchaseHistoryRepository.findRecentByBuffet(buffetId, 10))
                .thenReturn(List.of(pending));
        when(schoolAssistantResponder.respond(any())).thenReturn(Optional.of(
                new SchoolAssistantDraft("Tenes 7 sandwiches en stock.", "gemini-test")
        ));

        SchoolAssistantAnswer answer = askSchoolAssistant.executeForKiosquero(
                cognitoSubject,
                new SchoolAssistantCommand(Optional.empty(), "stock")
        );

        assertEquals("Tenes 7 sandwiches en stock.", answer.answer());
        assertTrue(answer.capabilities().contains(SchoolAssistantCapability.STOCK));
        assertTrue(answer.capabilities().contains(SchoolAssistantCapability.PEDIDOS));

        ArgumentCaptor<SchoolAssistantPrompt> promptCaptor = ArgumentCaptor
                .forClass(SchoolAssistantPrompt.class);
        verify(schoolAssistantResponder).respond(promptCaptor.capture());
        SchoolAssistantPrompt prompt = promptCaptor.getValue();
        assertEquals("KIOSQUERO", prompt.context().role().name());
        assertEquals(buffetId, prompt.context().buffetId());
        assertTrue(prompt.context().facts().stream().anyMatch(fact ->
                fact.contains("productoBuffet=") && fact.contains("Sandwich")));
    }

    @Test
    void executeForKiosqueroUsesFallbackWhenResponderDoesNotAnswer() {
        String cognitoSubject = "kiosquero-sub";
        UUID userId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        Usuario user = user(userId);

        when(usuarioRepository.findByIdCognito(cognitoSubject)).thenReturn(Optional.of(user));
        when(kiosqueroRepository.findBuffetByUsuarioId(userId)).thenReturn(buffetId);
        when(chatSessionRepository.startForUser(userId, NOW)).thenReturn(session);
        when(chatMessageRepository.findRecentBySessionId(sessionId, 6)).thenReturn(List.of());
        when(productRepository.findAllActiveWithStock(buffetId)).thenReturn(List.of());
        when(purchaseHistoryRepository.findByBuffetAndStatuses(
                eq(buffetId),
                any(),
                eq(10)
        )).thenReturn(List.of());
        when(purchaseHistoryRepository.findRecentByBuffet(buffetId, 10)).thenReturn(List.of());
        when(schoolAssistantResponder.respond(any())).thenReturn(Optional.empty());

        SchoolAssistantAnswer answer = askSchoolAssistant.executeForKiosquero(
                cognitoSubject,
                new SchoolAssistantCommand(Optional.empty(), "otra consulta")
        );

        assertEquals(
                "No pude responder ahora. Puedo ayudarte con stock, ventas y pedidos del buffet.",
                answer.answer()
        );
        assertEquals(SchoolAssistantRole.KIOSQUERO, answer.role());
        assertTrue(answer.suggestions().stream().anyMatch(suggestion ->
                suggestion.actionType() == SchoolAssistantActionType.CONSULTA_STOCK));
    }

    @Test
    void executeForTutorThrowsWhenAuthenticatedUserDoesNotExist() {
        String cognitoSubject = "missing-sub";
        when(usuarioRepository.findByIdCognito(cognitoSubject)).thenReturn(Optional.empty());

        assertThrows(
                UsuarioAutenticadoNoEncontradoException.class,
                () -> askSchoolAssistant.executeForTutor(
                        cognitoSubject,
                        new SchoolAssistantCommand(Optional.empty(), "hijos")
                )
        );
    }

    @Test
    void executeForTutorThrowsWhenTutorDoesNotExist() {
        String cognitoSubject = "tutor-sub";
        when(usuarioRepository.findByIdCognito(cognitoSubject))
                .thenReturn(Optional.of(user(UUID.randomUUID())));
        when(tutorRepository.findByUsuarioIdCognito(cognitoSubject)).thenReturn(Optional.empty());

        assertThrows(
                TutorNoEncontradoException.class,
                () -> askSchoolAssistant.executeForTutor(
                        cognitoSubject,
                        new SchoolAssistantCommand(Optional.empty(), "hijos")
                )
        );
    }

    @Test
    void executeForKiosqueroThrowsWhenBuffetDoesNotExist() {
        String cognitoSubject = "kiosquero-sub";
        UUID userId = UUID.randomUUID();
        when(usuarioRepository.findByIdCognito(cognitoSubject)).thenReturn(Optional.of(user(userId)));
        when(kiosqueroRepository.findBuffetByUsuarioId(userId)).thenReturn(null);

        assertThrows(
                KiosqueroNotFoundException.class,
                () -> askSchoolAssistant.executeForKiosquero(
                        cognitoSubject,
                        new SchoolAssistantCommand(Optional.empty(), "stock")
                )
        );
    }

    @Test
    void executeStoresActionMetadataFromFixedReply() {
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

        when(alumnoRepository.existsById(studentId)).thenReturn(true);
        when(chatSessionRepository.startForStudent(studentId, NOW)).thenReturn(session);
        when(chatMessageRepository.findRecentBySessionId(sessionId, 6)).thenReturn(List.of());
        when(walletRepository.findByStudentId(studentId)).thenReturn(Optional.empty());
        when(paymentRepository.findRecentByStudentId(studentId, 5)).thenReturn(List.of());
        when(purchaseRepository.findRecentByStudentId(studentId, 10)).thenReturn(List.of());
        when(schoolCalendarRepository.findByStudentIdAndDateRange(
                studentId,
                LocalDate.of(2026, 5, 27),
                LocalDate.of(2026, 6, 26)
        )).thenReturn(List.of());
        when(fixedTopics.respond(eq("confirmar"), any(), any())).thenReturn(Optional.of(fixedReply));

        askSchoolAssistant.execute(
                studentId,
                new SchoolAssistantCommand(Optional.empty(), "confirmar")
        );

        ArgumentCaptor<ChatMessage> messageCaptor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatMessageRepository, times(2)).save(messageCaptor.capture());
        Map<?, ?> metadata = messageCaptor.getAllValues().get(1).metadata();
        Map<?, ?> actionMetadata = (Map<?, ?>) metadata.get("accion");
        assertEquals("COMPRA_ANTICIPADA", actionMetadata.get("tipo"));
        assertEquals("EJECUTADA", actionMetadata.get("estado"));
        assertEquals(purchaseId.toString(), actionMetadata.get("compraId"));
        assertEquals("ABC123", actionMetadata.get("codigoRetiro"));
        assertEquals("FIRST_RECESS", actionMetadata.get("recreo"));
        assertEquals("CONFIRM_PURCHASE", metadata.get("intencionChatbot"));
    }

    private Billetera wallet(BigDecimal balance) {
        Billetera wallet = new Billetera();
        wallet.setSaldoCreditos(balance);
        return wallet;
    }

    private Usuario user(UUID userId) {
        Usuario user = new Usuario();
        user.setId(userId);
        return user;
    }
}
