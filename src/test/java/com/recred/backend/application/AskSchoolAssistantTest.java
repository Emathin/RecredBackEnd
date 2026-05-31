package com.recred.backend.application;

import com.recred.backend.domain.exceptions.ChatSessionNotFoundException;
import com.recred.backend.domain.exceptions.InvalidChatMessageException;
import com.recred.backend.domain.models.Wallet;
import com.recred.backend.domain.models.ChatMessage;
import com.recred.backend.domain.models.ChatSession;
import com.recred.backend.domain.models.ChatSessionState;
import com.recred.backend.domain.models.PurchaseState;
import com.recred.backend.domain.models.ExternalPaymentStatus;
import com.recred.backend.domain.models.PaymentMethod;
import com.recred.backend.domain.models.ChatMessageRole;
import com.recred.backend.domain.models.SchoolAssistantAnswer;
import com.recred.backend.domain.models.SchoolAssistantCapability;
import com.recred.backend.domain.models.SchoolAssistantDraft;
import com.recred.backend.domain.models.SchoolAssistantPayment;
import com.recred.backend.domain.models.SchoolAssistantPrompt;
import com.recred.backend.domain.models.SchoolAssistantPurchase;
import com.recred.backend.domain.models.SchoolCalendarEvent;
import com.recred.backend.domain.models.DayType;
import com.recred.backend.domain.repositories.StudentRepository;
import com.recred.backend.domain.repositories.ChatMessageRepository;
import com.recred.backend.domain.repositories.ChatSessionRepository;
import com.recred.backend.domain.repositories.SchoolAssistantPaymentRepository;
import com.recred.backend.domain.repositories.SchoolAssistantPurchaseRepository;
import com.recred.backend.domain.repositories.SchoolCalendarRepository;
import com.recred.backend.domain.repositories.WalletRepository;
import com.recred.backend.domain.services.SchoolAssistantResponder;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
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
    private StudentRepository studentRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private SchoolAssistantPaymentRepository paymentRepository;

    @Mock
    private SchoolAssistantPurchaseRepository purchaseRepository;

    @Mock
    private SchoolCalendarRepository schoolCalendarRepository;

    @Mock
    private ChatSessionRepository chatSessionRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private SchoolAssistantResponder schoolAssistantResponder;

    private AskSchoolAssistant askSchoolAssistant;
    private UUID studentId;
    private UUID sessionId;
    private ChatSession session;

    @BeforeEach
    void setUp() {
        askSchoolAssistant = new AskSchoolAssistant(
                studentRepository,
                walletRepository,
                paymentRepository,
                purchaseRepository,
                schoolCalendarRepository,
                chatSessionRepository,
                chatMessageRepository,
                schoolAssistantResponder,
                CLOCK
        );
        studentId = UUID.randomUUID();
        sessionId = UUID.randomUUID();
        session = new ChatSession(sessionId, ChatSessionState.ABIERTA, NOW, NOW);
    }

    @Test
    void executeBuildsFullContextAndReturnsAiAnswer() {
        Wallet wallet = wallet(new BigDecimal("1500.00"));
        SchoolAssistantPayment payment = new SchoolAssistantPayment(
                new BigDecimal("500.00"),
                ExternalPaymentStatus.APROBADO,
                NOW.minusDays(1),
                NOW.minusDays(1)
        );
        SchoolAssistantPurchase purchase = new SchoolAssistantPurchase(
                new BigDecimal("320.00"),
                PurchaseState.ENTREGADO,
                PaymentMethod.CREDITOS,
                NOW.minusHours(2)
        );
        SchoolCalendarEvent event = new SchoolCalendarEvent(
                LocalDate.of(2026, 5, 29),
                DayType.JORNADA_ESPECIAL,
                "Acto escolar"
        );

        when(studentRepository.existsById(studentId)).thenReturn(true);
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
        assertEquals(ChatMessageRole.USUARIO, messages.get(0).role());
        assertEquals(ChatMessageRole.ASISTENTE_IA, messages.get(1).role());
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
    void executeUsesFallbackWhenGeminiDoesNotReturnAnswer() {
        when(studentRepository.existsById(studentId)).thenReturn(true);
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
        verify(schoolAssistantResponder).respond(any());
    }

    @Test
    void executeThrowsWhenMessageIsBlank() {
        when(studentRepository.existsById(studentId)).thenReturn(true);

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
    void executeThrowsWhenRequestedSessionIsNotOpenForStudent() {
        when(studentRepository.existsById(studentId)).thenReturn(true);
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

    private Wallet wallet(BigDecimal balance) {
        Wallet wallet = new Wallet();
        wallet.setCreditBalance(balance);
        return wallet;
    }
}
