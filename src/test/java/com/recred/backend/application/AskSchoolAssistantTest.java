package com.recred.backend.application;

import com.recred.backend.domain.exceptions.ChatSessionNotFoundException;
import com.recred.backend.domain.exceptions.InvalidChatMessageException;
import com.recred.backend.domain.models.Billetera;
import com.recred.backend.domain.models.ChatMessage;
import com.recred.backend.domain.models.ChatSession;
import com.recred.backend.domain.models.DateRange;
import com.recred.backend.domain.models.EstadoChatSesion;
import com.recred.backend.domain.models.EstadoPagoExterno;
import com.recred.backend.domain.models.RolChatMensaje;
import com.recred.backend.domain.models.SchoolAssistantAnswer;
import com.recred.backend.domain.models.SchoolAssistantCapability;
import com.recred.backend.domain.models.SchoolAssistantDraft;
import com.recred.backend.domain.models.SchoolAssistantPayment;
import com.recred.backend.domain.models.SchoolAssistantPrompt;
import com.recred.backend.domain.models.SchoolAssistantQuery;
import com.recred.backend.domain.models.SchoolCalendarEvent;
import com.recred.backend.domain.models.TipoDiaEscolar;
import com.recred.backend.domain.repositories.AlumnoRepository;
import com.recred.backend.domain.repositories.ChatMessageRepository;
import com.recred.backend.domain.repositories.ChatSessionRepository;
import com.recred.backend.domain.repositories.SchoolAssistantPaymentRepository;
import com.recred.backend.domain.repositories.SchoolCalendarRepository;
import com.recred.backend.domain.repositories.WalletRepository;
import com.recred.backend.domain.services.SchoolAssistantQueryInterpreter;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AskSchoolAssistantTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-05-27T12:00:00Z"),
            ZoneOffset.UTC
    );
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 27, 12, 0);

    @Mock
    private AlumnoRepository alumnoRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private SchoolAssistantPaymentRepository paymentRepository;

    @Mock
    private SchoolCalendarRepository schoolCalendarRepository;

    @Mock
    private ChatSessionRepository chatSessionRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private SchoolAssistantQueryInterpreter schoolAssistantQueryInterpreter;

    @Mock
    private SchoolAssistantResponder schoolAssistantResponder;

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
                schoolCalendarRepository,
                chatSessionRepository,
                chatMessageRepository,
                schoolAssistantQueryInterpreter,
                schoolAssistantResponder,
                CLOCK
        );
        studentId = UUID.randomUUID();
        sessionId = UUID.randomUUID();
        session = new ChatSession(sessionId, EstadoChatSesion.ABIERTA, NOW, NOW);
    }

    @Test
    void executeBuildsContextAndReturnsAiAnswer() {
        Billetera wallet = wallet(new BigDecimal("1500.00"));
        SchoolAssistantPayment payment = new SchoolAssistantPayment(
                new BigDecimal("500.00"),
                EstadoPagoExterno.APROBADO,
                NOW.minusDays(1),
                NOW.minusDays(1)
        );
        SchoolCalendarEvent event = new SchoolCalendarEvent(
                LocalDate.of(2026, 5, 29),
                TipoDiaEscolar.JORNADA_ESPECIAL,
                "Acto escolar"
        );

        when(alumnoRepository.existsById(studentId)).thenReturn(true);
        when(chatSessionRepository.startForStudent(studentId, NOW)).thenReturn(session);
        when(chatMessageRepository.findRecentBySessionId(sessionId, 6)).thenReturn(List.of());
        when(schoolAssistantQueryInterpreter.interpret(
                eq("Quiero ver saldo, pagos y eventos"),
                eq(LocalDate.of(2026, 5, 27)),
                eq(List.of())
        )).thenReturn(Optional.of(new SchoolAssistantQuery(
                List.of(
                        SchoolAssistantCapability.SALDO,
                        SchoolAssistantCapability.PAGOS,
                        SchoolAssistantCapability.EVENTOS
                ),
                Optional.of(new DateRange(
                        LocalDate.of(2026, 5, 1),
                        LocalDate.of(2026, 5, 31)
                )),
                Optional.of(new DateRange(
                        LocalDate.of(2026, 5, 29),
                        LocalDate.of(2026, 5, 29)
                )),
                3,
                true
        )));
        when(walletRepository.findByStudentId(studentId)).thenReturn(Optional.of(wallet));
        when(paymentRepository.findByStudentIdAndDateRange(
                studentId,
                LocalDateTime.of(2026, 5, 1, 0, 0),
                LocalDateTime.of(2026, 5, 31, 23, 59, 59, 999999999),
                3
        )).thenReturn(List.of(payment));
        when(schoolCalendarRepository.findByStudentIdAndDateRange(
                studentId,
                LocalDate.of(2026, 5, 29),
                LocalDate.of(2026, 5, 29)
        )).thenReturn(List.of(event));
        when(schoolAssistantResponder.respond(any())).thenReturn(Optional.of(
                new SchoolAssistantDraft("Respuesta IA.", "gemini-test")
        ));

        SchoolAssistantAnswer answer = askSchoolAssistant.execute(
                studentId,
                new SchoolAssistantCommand(
                        Optional.empty(),
                        "Quiero ver saldo, pagos y eventos"
                )
        );

        assertEquals(sessionId, answer.sessionId());
        assertEquals("Respuesta IA.", answer.answer());
        assertTrue(answer.generatedByAi());
        assertEquals("gemini-test", answer.model());
        assertEquals(List.of(
                SchoolAssistantCapability.SALDO,
                SchoolAssistantCapability.PAGOS,
                SchoolAssistantCapability.EVENTOS
        ), answer.capabilities());

        ArgumentCaptor<SchoolAssistantPrompt> promptCaptor = ArgumentCaptor
                .forClass(SchoolAssistantPrompt.class);
        verify(schoolAssistantResponder).respond(promptCaptor.capture());
        SchoolAssistantPrompt prompt = promptCaptor.getValue();
        assertEquals(new BigDecimal("1500.00"), prompt.context().balance().orElseThrow());
        assertEquals(payment, prompt.context().recentPayments().get(0));
        assertEquals(event, prompt.context().upcomingEvents().get(0));
        assertEquals(3, prompt.query().paymentLimit());
        assertTrue(prompt.recentMessages().isEmpty());

        ArgumentCaptor<ChatMessage> messageCaptor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatMessageRepository, times(2)).save(messageCaptor.capture());
        List<ChatMessage> messages = messageCaptor.getAllValues();
        assertEquals(RolChatMensaje.USUARIO, messages.get(0).role());
        assertEquals(RolChatMensaje.ASISTENTE_IA, messages.get(1).role());
        assertEquals("Respuesta IA.", messages.get(1).content());
        assertEquals(true, messages.get(1).metadata().get("generadoPorIa"));
        verify(chatSessionRepository).updateLastActivity(sessionId, NOW);
    }

    @Test
    void executeUsesTemplateWhenAiResponseIsNotRequired() {
        when(alumnoRepository.existsById(studentId)).thenReturn(true);
        when(chatSessionRepository.startForStudent(studentId, NOW)).thenReturn(session);
        when(chatMessageRepository.findRecentBySessionId(sessionId, 6)).thenReturn(List.of());
        when(schoolAssistantQueryInterpreter.interpret(
                eq("saldo"),
                eq(LocalDate.of(2026, 5, 27)),
                eq(List.of())
        )).thenReturn(Optional.of(new SchoolAssistantQuery(
                List.of(SchoolAssistantCapability.SALDO),
                Optional.empty(),
                Optional.empty(),
                5,
                false
        )));
        when(walletRepository.findByStudentId(studentId)).thenReturn(Optional.of(
                wallet(new BigDecimal("123.45"))
        ));

        SchoolAssistantAnswer answer = askSchoolAssistant.execute(
                studentId,
                new SchoolAssistantCommand(Optional.empty(), "saldo")
        );

        assertEquals("Saldo disponible: $123.45.", answer.answer());
        assertFalse(answer.generatedByAi());
        assertEquals(List.of(SchoolAssistantCapability.SALDO), answer.capabilities());
        verifyNoInteractions(schoolAssistantResponder);
    }

    @Test
    void executeDoesNotCallGeminiForOutOfScopeQuestions() {
        when(alumnoRepository.existsById(studentId)).thenReturn(true);
        when(chatSessionRepository.startForStudent(studentId, NOW)).thenReturn(session);
        when(chatMessageRepository.findRecentBySessionId(sessionId, 6)).thenReturn(List.of());
        when(schoolAssistantQueryInterpreter.interpret(
                eq("hola"),
                eq(LocalDate.of(2026, 5, 27)),
                eq(List.of())
        )).thenReturn(Optional.of(new SchoolAssistantQuery(
                List.of(SchoolAssistantCapability.FUERA_DE_ALCANCE),
                Optional.empty(),
                Optional.empty(),
                5,
                false
        )));

        SchoolAssistantAnswer answer = askSchoolAssistant.execute(
                studentId,
                new SchoolAssistantCommand(Optional.empty(), "hola")
        );

        assertEquals(
                "Puedo ayudarte con consultas de saldo, pagos y eventos escolares.",
                answer.answer()
        );
        assertEquals(
                List.of(SchoolAssistantCapability.FUERA_DE_ALCANCE),
                answer.capabilities()
        );
        verifyNoInteractions(walletRepository);
        verifyNoInteractions(paymentRepository);
        verifyNoInteractions(schoolCalendarRepository);
        verifyNoInteractions(schoolAssistantResponder);
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

    private Billetera wallet(BigDecimal balance) {
        Billetera wallet = new Billetera();
        wallet.setSaldoCreditos(balance);
        return wallet;
    }
}
