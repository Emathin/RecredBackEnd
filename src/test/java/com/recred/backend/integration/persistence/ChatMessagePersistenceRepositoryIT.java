package com.recred.backend.integration.persistence;

import com.recred.backend.domain.models.ChatMessage;
import com.recred.backend.domain.models.RolChatMensaje;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.TipoPlan;
import com.recred.backend.infrastructure.entities.ChatMensajeEntity;
import com.recred.backend.infrastructure.entities.ChatSesionEntity;
import com.recred.backend.infrastructure.entities.UsuarioEntity;
import com.recred.backend.domain.models.EstadoChatSesion;
import com.recred.backend.infrastructure.persistence.ChatMessagePersistenceRepository;
import com.recred.backend.infrastructure.persistence.SpringDataChatMessageRepository;
import com.recred.backend.integration.AbstractIntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.time.temporal.ChronoUnit.MILLIS;
import static org.junit.jupiter.api.Assertions.*;

@Transactional
class ChatMessagePersistenceRepositoryIT extends AbstractIntegrationTest {

    @Autowired
    private SpringDataChatMessageRepository chatMessageRepository;

    @Autowired
    private ChatMessagePersistenceRepository repository;

    @Autowired
    private EntityManager entityManager;

    private UsuarioEntity cachedUser;
    private ChatSesionEntity cachedSession;
    private ChatMensajeEntity cachedMessage;

    @Test
    void save_ShouldMapMessageToEntity() {
        ChatSesionEntity session = givenSessionEntity();
        LocalDateTime now = LocalDateTime.now().truncatedTo(MILLIS);

        whenSavingMessage(session, now);

        thenMessageIsPersisted(session, now);
    }

    @Test
    void findRecentBySessionId_ShouldMapMessagesToDomain() {
        ChatMensajeEntity messageEntity = givenMessageEntity();

        List<ChatMessage> messages = whenFindingRecentMessages(messageEntity.getSesion(), 2);

        thenMessagesAreMappedProperly(messages, messageEntity);
    }

    @Test
    void findBySessionId_ShouldMapPagedMessagesToDomain() {
        ChatMensajeEntity messageEntity = givenMessageEntity();

        List<ChatMessage> messages = whenFindingMessagesBySessionPaged(messageEntity.getSesion(), 0, 5);

        thenPagedMessagesAreMappedProperly(messages, messageEntity);
    }

    @Test
    void deleteBySessionId_ShouldRemoveEntities() {
        ChatMensajeEntity messageEntity = givenMessageEntity();

        whenDeletingMessagesBySession(messageEntity.getSesion());

        thenMessagesAreDeleted(messageEntity);
    }

    private ChatSesionEntity givenSessionEntity() {
        return generateSessionEntity();
    }

    private ChatMensajeEntity givenMessageEntity() {
        return generateMessageEntity();
    }

    private void whenSavingMessage(ChatSesionEntity session, LocalDateTime timestamp) {
        repository.save(new ChatMessage(
                session.getId(),
                RolChatMensaje.USUARIO,
                "balance request",
                Map.of("capabilities", List.of("BALANCE")),
                timestamp
        ));
    }

    private List<ChatMessage> whenFindingRecentMessages(ChatSesionEntity session, int limit) {
        return repository.findRecentBySessionId(session.getId(), limit);
    }

    private List<ChatMessage> whenFindingMessagesBySessionPaged(ChatSesionEntity session, int offset, int limit) {
        return repository.findBySessionId(session.getId(), offset, limit);
    }

    private void whenDeletingMessagesBySession(ChatSesionEntity session) {
        repository.deleteBySessionId(session.getId());
        entityManager.flush();
        entityManager.clear();
    }

    private void thenMessageIsPersisted(ChatSesionEntity session, LocalDateTime timestamp) {
        List<ChatMensajeEntity> messages = chatMessageRepository.findAll();
        assertFalse(messages.isEmpty());
        
        Optional<ChatMensajeEntity> savedOpt = messages.stream()
                .filter(m -> "balance request".equals(m.getContenido()))
                .findFirst();
                
        assertTrue(savedOpt.isPresent());
        ChatMensajeEntity saved = savedOpt.get();
        assertEquals(session.getId(), saved.getSesion().getId());
        assertEquals(RolChatMensaje.USUARIO, saved.getRol());
        assertEquals(timestamp, saved.getFechaHora());
    }

    private void thenMessagesAreMappedProperly(List<ChatMessage> messages, ChatMensajeEntity messageEntity) {
        assertFalse(messages.isEmpty());
        assertEquals(messageEntity.getSesion().getId(), messages.get(0).sessionId());
        assertEquals(messageEntity.getContenido(), messages.get(0).content());
        assertEquals(messageEntity.getRol(), messages.get(0).role());
    }

    private void thenPagedMessagesAreMappedProperly(List<ChatMessage> messages, ChatMensajeEntity messageEntity) {
        assertFalse(messages.isEmpty());
        assertEquals(messageEntity.getId(), messages.get(0).id());
        assertEquals(messageEntity.getSesion().getId(), messages.get(0).sessionId());
        assertEquals(messageEntity.getRol(), messages.get(0).role());
    }

    private void thenMessagesAreDeleted(ChatMensajeEntity messageEntity) {
        assertTrue(chatMessageRepository.findById(messageEntity.getId()).isEmpty());
    }

    private UsuarioEntity generateUserEntity() {
        if (this.cachedUser == null) {
            UsuarioEntity user = new UsuarioEntity();
            user.setIdCognito("cognito-chat-message");
            user.setNombre("Alice");
            user.setApellido("Smith");
            user.setEmail("alice@chat.com");
            user.setRol(RolUsuario.ALUMNO);
            user.setAutenticadoConCognito(true);
            user.setActivo(true);
            user.setPlan(TipoPlan.GRATUITO);
            entityManager.persist(user);
            entityManager.flush();
            this.cachedUser = user;
        }
        return this.cachedUser;
    }

    private ChatSesionEntity generateSessionEntity() {
        if (this.cachedSession == null) {
            ChatSesionEntity session = new ChatSesionEntity();
            session.setUsuarioEntity(generateUserEntity());
            session.setEstado(EstadoChatSesion.ABIERTA);
            session.setFechaInicio(LocalDateTime.now().minusHours(2));
            session.setFechaUltimaActividad(LocalDateTime.now().minusHours(1));
            entityManager.persist(session);
            entityManager.flush();
            this.cachedSession = session;
        }
        return this.cachedSession;
    }

    private ChatMensajeEntity generateMessageEntity() {
        if (this.cachedMessage == null) {
            ChatMensajeEntity message = new ChatMensajeEntity();
            message.setSesion(generateSessionEntity());
            message.setRol(RolChatMensaje.USUARIO);
            message.setContenido("Hello world");
            message.setFechaHora(LocalDateTime.now());
            entityManager.persist(message);
            entityManager.flush();
            this.cachedMessage = message;
        }
        return this.cachedMessage;
    }
}
