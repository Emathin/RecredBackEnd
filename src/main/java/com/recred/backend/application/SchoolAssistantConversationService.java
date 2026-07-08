package com.recred.backend.application;

import com.recred.backend.domain.exceptions.ChatSessionNotFoundException;
import com.recred.backend.domain.models.ChatMessage;
import com.recred.backend.domain.models.ChatSession;
import com.recred.backend.domain.models.RolChatMensaje;
import com.recred.backend.domain.models.SchoolAssistantAnswer;
import com.recred.backend.domain.models.SchoolAssistantCapability;
import com.recred.backend.domain.repositories.ChatMessageRepository;
import com.recred.backend.domain.repositories.ChatSessionRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SchoolAssistantConversationService {

    private static final int RECENT_HISTORY_LIMIT = 6;

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final SchoolAssistantMessageMetadataMapper metadataMapper;

    public SchoolAssistantConversationService(
            ChatSessionRepository chatSessionRepository,
            ChatMessageRepository chatMessageRepository
    ) {
        this.chatSessionRepository = chatSessionRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.metadataMapper = new SchoolAssistantMessageMetadataMapper();
    }

    public SchoolAssistantConversation startForStudent(
            UUID studentId,
            UUID sessionId,
            LocalDateTime now
    ) {
        return conversation(studentSession(studentId, sessionId, now));
    }

    public SchoolAssistantConversation start(
            SchoolAssistantConversationOwner owner,
            UUID sessionId,
            LocalDateTime now
    ) {
        return owner.isStudent()
                ? startForStudent(owner.id(), sessionId, now)
                : startForUser(owner.id(), sessionId, now);
    }

    public SchoolAssistantConversation startForUser(
            UUID userId,
            UUID sessionId,
            LocalDateTime now
    ) {
        return conversation(userSession(userId, sessionId, now));
    }

    public void saveConversation(
            SchoolAssistantConversation conversation,
            String userMessage,
            List<SchoolAssistantCapability> capabilities,
            SchoolAssistantAnswer answer,
            Map<String, Object> additionalMetadata,
            LocalDateTime now
    ) {
        saveUserMessage(conversation.session(), userMessage, capabilities, now);
        saveAssistantMessage(answer, additionalMetadata, now);
        chatSessionRepository.updateLastActivity(conversation.sessionId(), now);
    }

    private SchoolAssistantConversation conversation(ChatSession session) {
        return new SchoolAssistantConversation(
                session,
                chatMessageRepository.findRecentBySessionId(
                        session.id(),
                        RECENT_HISTORY_LIMIT
                )
        );
    }

    private ChatSession studentSession(UUID studentId, UUID sessionId, LocalDateTime now) {
        return sessionId == null
                ? chatSessionRepository.startForStudent(studentId, now)
                : existingStudentSession(studentId, sessionId);
    }

    private ChatSession userSession(UUID userId, UUID sessionId, LocalDateTime now) {
        return sessionId == null
                ? chatSessionRepository.startForUser(userId, now)
                : existingUserSession(userId, sessionId);
    }

    private ChatSession existingStudentSession(UUID studentId, UUID sessionId) {
        return chatSessionRepository.findOpenByIdAndStudentId(sessionId, studentId)
                .orElseThrow(() -> new ChatSessionNotFoundException(sessionId));
    }

    private ChatSession existingUserSession(UUID userId, UUID sessionId) {
        return chatSessionRepository.findOpenByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new ChatSessionNotFoundException(sessionId));
    }

    private void saveUserMessage(
            ChatSession session,
            String message,
            List<SchoolAssistantCapability> capabilities,
            LocalDateTime now
    ) {
        chatMessageRepository.save(new ChatMessage(
                session.id(),
                RolChatMensaje.USUARIO,
                message,
                metadataMapper.userMetadata(capabilities),
                now
        ));
    }

    private void saveAssistantMessage(
            SchoolAssistantAnswer answer,
            Map<String, Object> additionalMetadata,
            LocalDateTime now
    ) {
        chatMessageRepository.save(new ChatMessage(
                answer.sessionId(),
                RolChatMensaje.ASISTENTE_IA,
                answer.answer(),
                metadataMapper.answerMetadata(answer, additionalMetadata),
                now
        ));
    }
}
