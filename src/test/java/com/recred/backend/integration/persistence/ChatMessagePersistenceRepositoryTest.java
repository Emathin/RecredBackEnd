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

import static org.junit.jupiter.api.Assertions.*;

@Transactional
class ChatMessagePersistenceRepositoryTest extends AbstractIntegrationTest {

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
        ChatSesionEntity session = generateSessionEntity();
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);

        repository.save(new ChatMessage(
                session.getId(),
                RolChatMensaje.USUARIO,
                "balance request",
                Map.of("capabilities", List.of("BALANCE")),
                now
        ));

        List<ChatMensajeEntity> messages = chatMessageRepository.findAll();
        assertFalse(messages.isEmpty());
        
        Optional<ChatMensajeEntity> savedOpt = messages.stream()
                .filter(m -> "balance request".equals(m.getContenido()))
                .findFirst();
                
        assertTrue(savedOpt.isPresent());
        ChatMensajeEntity saved = savedOpt.get();
        assertEquals(session.getId(), saved.getSesion().getId());
        assertEquals(RolChatMensaje.USUARIO, saved.getRol());
        assertEquals(now, saved.getFechaHora());
    }

    @Test
    void findRecentBySessionId_ShouldMapMessagesToDomain() {
        ChatMensajeEntity messageEntity = generateMessageEntity();

        List<ChatMessage> messages = repository.findRecentBySessionId(messageEntity.getSesion().getId(), 2);

        assertFalse(messages.isEmpty());
        assertEquals(messageEntity.getSesion().getId(), messages.get(0).sessionId());
        assertEquals(messageEntity.getContenido(), messages.get(0).content());
        assertEquals(messageEntity.getRol(), messages.get(0).role());
    }

    @Test
    void findBySessionId_ShouldMapPagedMessagesToDomain() {
        ChatMensajeEntity messageEntity = generateMessageEntity();

        List<ChatMessage> messages = repository.findBySessionId(messageEntity.getSesion().getId(), 0, 5);

        assertFalse(messages.isEmpty());
        assertEquals(messageEntity.getId(), messages.get(0).id());
        assertEquals(messageEntity.getSesion().getId(), messages.get(0).sessionId());
        assertEquals(messageEntity.getRol(), messages.get(0).role());
    }

    @Test
    void deleteBySessionId_ShouldRemoveEntities() {
        ChatMensajeEntity messageEntity = generateMessageEntity();

        repository.deleteBySessionId(messageEntity.getSesion().getId());
        entityManager.flush();
        entityManager.clear();

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
