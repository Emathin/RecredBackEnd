package com.recred.backend.application;

import com.recred.backend.domain.models.ChatMessage;
import com.recred.backend.domain.models.RecessTime;
import com.recred.backend.domain.models.RolChatMensaje;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SchoolAssistantPendingPurchaseParserTest {

    private final SchoolAssistantPendingPurchaseParser parser =
            new SchoolAssistantPendingPurchaseParser(new SchoolAssistantIntentDetector());

    @Test
    void pendingPurchaseReadsFirstAssistantQuickPurchaseAction() {
        UUID studentId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        ChatMessage userMessage = message(RolChatMensaje.USUARIO, Map.of());
        ChatMessage assistantMessage = message(RolChatMensaje.ASISTENTE_IA, Map.of(
                "accionPendienteChatbot",
                Map.of(
                        "tipo", "COMPRA_RAPIDA",
                        "alumnoId", studentId.toString(),
                        "buffetId", buffetId.toString(),
                        "recreo", "primer recreo",
                        "items", List.of(
                                Map.of("productoId", productId.toString(), "cantidad", "2"),
                                Map.of("productoId", UUID.randomUUID().toString(), "cantidad", 0),
                                "ignorado"
                        )
                )
        ));

        Optional<PendingPurchase> result =
                parser.pendingPurchase(List.of(userMessage, assistantMessage));

        assertTrue(result.isPresent());
        assertEquals(studentId, result.orElseThrow().studentId());
        assertEquals(buffetId, result.orElseThrow().buffetId());
        assertEquals(RecessTime.FIRST_RECESS, result.orElseThrow().recessTime().orElseThrow());
        assertEquals(1, result.orElseThrow().items().size());
        assertEquals(productId, result.orElseThrow().items().get(0).productId());
        assertEquals(2, result.orElseThrow().items().get(0).quantity());
    }

    @Test
    void pendingPurchaseStopsWhenLatestAssistantActionIsTerminal() {
        UUID studentId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        ChatMessage terminalMessage = message(RolChatMensaje.ASISTENTE_IA, Map.of(
                "estadoAccionChatbot", "EJECUTADA"
        ));
        ChatMessage pendingMessage = message(RolChatMensaje.ASISTENTE_IA, Map.of(
                "accionPendienteChatbot",
                Map.of(
                        "tipo", "COMPRA_RAPIDA",
                        "alumnoId", studentId.toString(),
                        "buffetId", buffetId.toString(),
                        "items", List.of(Map.of(
                                "productoId", productId.toString(),
                                "cantidad", 1
                        ))
                )
        ));

        Optional<PendingPurchase> result =
                parser.pendingPurchase(List.of(terminalMessage, pendingMessage));

        assertTrue(result.isEmpty());
    }

    @Test
    void pendingPurchaseIgnoresMalformedActionsAndKeepsSearching() {
        UUID studentId = UUID.randomUUID();
        UUID buffetId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        ChatMessage malformed = message(RolChatMensaje.ASISTENTE_IA, Map.of(
                "accionPendienteChatbot",
                Map.of(
                        "tipo", "COMPRA_RAPIDA",
                        "alumnoId", "no-es-uuid",
                        "buffetId", buffetId.toString(),
                        "items", List.of(Map.of(
                                "productoId", productId.toString(),
                                "cantidad", "no-es-numero"
                        ))
                )
        ));
        ChatMessage nonMapAction = message(RolChatMensaje.ASISTENTE_IA, Map.of(
                "accionPendienteChatbot", "sin-accion"
        ));
        ChatMessage valid = message(RolChatMensaje.ASISTENTE_IA, Map.of(
                "accionPendienteChatbot",
                Map.of(
                        "tipo", "COMPRA_RAPIDA",
                        "alumnoId", studentId.toString(),
                        "buffetId", buffetId.toString(),
                        "items", List.of(Map.of(
                                "productoId", productId.toString(),
                                "cantidad", 1
                        ))
                )
        ));

        Optional<PendingPurchase> result =
                parser.pendingPurchase(List.of(malformed, nonMapAction, valid));

        assertTrue(result.isPresent());
        assertEquals(studentId, result.orElseThrow().studentId());
        assertEquals(1, result.orElseThrow().items().get(0).quantity());
    }

    @Test
    void pendingPurchaseReturnsEmptyWhenItemsAreNotAList() {
        ChatMessage message = message(RolChatMensaje.ASISTENTE_IA, Map.of(
                "accionPendienteChatbot",
                Map.of(
                        "tipo", "COMPRA_RAPIDA",
                        "alumnoId", UUID.randomUUID().toString(),
                        "buffetId", UUID.randomUUID().toString(),
                        "items", "sin-items"
                )
        ));

        Optional<PendingPurchase> result = parser.pendingPurchase(List.of(message));

        assertTrue(result.isEmpty());
    }

    private ChatMessage message(RolChatMensaje role, Map<String, Object> metadata) {
        return new ChatMessage(
                UUID.randomUUID(),
                UUID.randomUUID(),
                role,
                "mensaje",
                metadata,
                LocalDateTime.of(2026, 6, 15, 12, 0)
        );
    }
}
