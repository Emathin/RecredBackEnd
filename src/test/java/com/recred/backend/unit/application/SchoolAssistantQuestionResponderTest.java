package com.recred.backend.unit.application;

import com.recred.backend.application.SchoolAssistantAnswerResult;
import com.recred.backend.application.SchoolAssistantAnswerFormatter;
import com.recred.backend.application.SchoolAssistantConversation;
import com.recred.backend.application.SchoolAssistantFixedReply;
import com.recred.backend.application.SchoolAssistantFixedTopics;
import com.recred.backend.application.SchoolAssistantQuestionResponder;
import com.recred.backend.application.SchoolAssistantRoleCatalog;
import com.recred.backend.domain.models.ChatMessage;
import com.recred.backend.domain.models.ChatSession;
import com.recred.backend.domain.models.EstadoChatSesion;
import com.recred.backend.domain.models.RolChatMensaje;
import com.recred.backend.domain.models.SchoolAssistantDraft;
import com.recred.backend.domain.models.SchoolAssistantIntent;
import com.recred.backend.domain.models.SchoolAssistantPrompt;
import com.recred.backend.domain.models.SchoolAssistantRole;
import com.recred.backend.domain.models.SchoolAssistantContext;
import com.recred.backend.domain.services.SchoolAssistantResponder;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchoolAssistantQuestionResponderTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 27, 12, 0);

    @Mock
    private SchoolAssistantFixedTopics fixedTopics;

    @Mock
    private SchoolAssistantResponder schoolAssistantResponder;

    private SchoolAssistantQuestionResponder questionResponder;
    private UUID studentId;
    private UUID sessionId;
    private SchoolAssistantContext context;
    private SchoolAssistantConversation conversation;
    private List<ChatMessage> recentMessages;
    private SchoolAssistantFixedReply fixedReply;
    private SchoolAssistantAnswerResult result;

    @BeforeEach
    void setUp() {
        questionResponder = new SchoolAssistantQuestionResponder(
                fixedTopics,
                schoolAssistantResponder,
                new SchoolAssistantAnswerFormatter(),
                new SchoolAssistantRoleCatalog()
        );
        studentId = UUID.randomUUID();
        sessionId = UUID.randomUUID();
        recentMessages = List.of(userMessage("saldo"));
        context = studentContext();
        conversation = new SchoolAssistantConversation(session(), recentMessages);
    }

    @Test
    void respond_WhenFixedReplyExists_ShouldReturnFixedAnswerAndMetadata() {
        givenFixedBalanceReply();

        whenRespondingToQuestion("saldo");

        thenFixedAnswerWasReturned();
        thenFixedMetadataWasReturned();
        thenAiWasNotRequested();
    }

    @Test
    void respond_WhenFixedReplyDoesNotExist_ShouldUseAiAnswer() {
        givenNoFixedReply();
        givenAiAnswer("Respuesta IA.", "gemini-test");

        whenRespondingToQuestion("otra consulta");

        thenAiAnswerWasReturned();
        thenAiPromptWasSent("otra consulta");
        thenNoAdditionalMetadataWasReturned();
    }

    @Test
    void respond_WhenAiDoesNotAnswer_ShouldUseRoleFallback() {
        givenNoFixedReply();
        givenNoAiAnswer();

        whenRespondingToQuestion("otra consulta");

        thenStudentFallbackWasReturned();
        thenNoAdditionalMetadataWasReturned();
    }

    @Test
    void respond_WhenAiAnswerNeedsFormatting_ShouldReturnFormattedAnswer() {
        givenNoFixedReply();
        givenAiAnswer(rawStockAnswer(), "gemini-test");

        whenRespondingToQuestion("stock");

        thenFormattedAnswerWasReturned();
    }

    private void givenFixedBalanceReply() {
        fixedReply = new SchoolAssistantFixedReply(
                "Tenes $1500.00 disponibles.",
                Map.of("intencionChatbot", "BALANCE"),
                SchoolAssistantIntent.BALANCE,
                null,
                List.of(),
                Map.of("saldo", new BigDecimal("1500.00"))
        );
        when(fixedTopics.respond(eq("saldo"), eq(context), eq(recentMessages)))
                .thenReturn(Optional.of(fixedReply));
    }

    private void givenNoFixedReply() {
        when(fixedTopics.respond(any(), eq(context), eq(recentMessages)))
                .thenReturn(Optional.empty());
    }

    private void givenAiAnswer(String answer, String model) {
        when(schoolAssistantResponder.respond(any())).thenReturn(Optional.of(
                new SchoolAssistantDraft(answer, model)
        ));
    }

    private void givenNoAiAnswer() {
        when(schoolAssistantResponder.respond(any())).thenReturn(Optional.empty());
    }

    private void whenRespondingToQuestion(String message) {
        result = questionResponder.respond(conversation, message, context, NOW);
    }

    private void thenFixedAnswerWasReturned() {
        assertEquals("Tenes $1500.00 disponibles.", result.answer().answer());
        assertEquals(SchoolAssistantIntent.BALANCE, result.answer().intent());
        assertFalse(result.answer().generatedByAi());
        assertEquals(new BigDecimal("1500.00"), result.answer().data().get("saldo"));
    }

    private void thenFixedMetadataWasReturned() {
        assertEquals("BALANCE", result.additionalMetadata().get("intencionChatbot"));
    }

    private void thenAiWasNotRequested() {
        verify(schoolAssistantResponder, never()).respond(any());
    }

    private void thenAiAnswerWasReturned() {
        assertEquals("Respuesta IA.", result.answer().answer());
        assertTrue(result.answer().generatedByAi());
        assertEquals("gemini-test", result.answer().model());
        assertEquals(SchoolAssistantIntent.UNKNOWN, result.answer().intent());
    }

    private void thenAiPromptWasSent(String expectedQuestion) {
        ArgumentCaptor<SchoolAssistantPrompt> promptCaptor =
                ArgumentCaptor.forClass(SchoolAssistantPrompt.class);
        verify(schoolAssistantResponder).respond(promptCaptor.capture());
        SchoolAssistantPrompt prompt = promptCaptor.getValue();
        assertEquals(expectedQuestion, prompt.question());
        assertEquals(context, prompt.context());
        assertEquals(recentMessages, prompt.recentMessages());
    }

    private void thenNoAdditionalMetadataWasReturned() {
        assertTrue(result.additionalMetadata().isEmpty());
    }

    private void thenStudentFallbackWasReturned() {
        assertEquals(
                "No pude responder ahora. Puedo ayudarte con saldos, pagos, compras "
                        + "y eventos escolares.",
                result.answer().answer()
        );
        assertFalse(result.answer().generatedByAi());
        assertTrue(result.answer().suggestionsOrEmpty().stream().anyMatch(suggestion ->
                "Compra habitual".equals(suggestion.label())));
    }

    private void thenFormattedAnswerWasReturned() {
        assertEquals("""
                Stock actual:
                - Coca-Cola 500ml por $1200.00 (stock: 50)
                - Jugo de Naranja 300ml por $950.00 (stock: 35)
                - Papas Chips 50g por $600.00 (stock: 70)
                """.trim(), result.answer().answer());
    }

    private String rawStockAnswer() {
        return "Stock actual: Coca-Cola 500ml por $1200.00 (stock: 50); "
                + "Jugo de Naranja 300ml por $950.00 (stock: 35); "
                + "Papas Chips 50g por $600.00 (stock: 70).";
    }

    private SchoolAssistantContext studentContext() {
        return new SchoolAssistantContext(
                studentId,
                Optional.of(new BigDecimal("1500.00")),
                List.of(),
                List.of(),
                List.of()
        );
    }

    private ChatSession session() {
        return new ChatSession(sessionId, EstadoChatSesion.ABIERTA, NOW, NOW);
    }

    private ChatMessage userMessage(String content) {
        return new ChatMessage(
                sessionId,
                RolChatMensaje.USUARIO,
                content,
                Map.of(),
                NOW
        );
    }
}
