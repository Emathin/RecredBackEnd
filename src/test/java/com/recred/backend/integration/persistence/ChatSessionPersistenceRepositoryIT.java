package com.recred.backend.integration.persistence;

import com.recred.backend.domain.exceptions.AlumnoNoEncontradoException;
import com.recred.backend.domain.models.ChatSession;
import com.recred.backend.domain.models.EstadoChatSesion;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.TipoPlan;
import com.recred.backend.infrastructure.entities.AlumnoEntity;
import com.recred.backend.infrastructure.entities.ChatSesionEntity;
import com.recred.backend.infrastructure.entities.UsuarioEntity;
import com.recred.backend.infrastructure.persistence.ChatSessionPersistenceRepository;
import com.recred.backend.infrastructure.persistence.SpringDataChatSessionRepository;
import com.recred.backend.integration.AbstractIntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.time.temporal.ChronoUnit.MILLIS;
import static org.junit.jupiter.api.Assertions.*;

@Transactional
class ChatSessionPersistenceRepositoryIT extends AbstractIntegrationTest {

    @Autowired
    private SpringDataChatSessionRepository chatSessionRepository;

    @Autowired
    private ChatSessionPersistenceRepository repository;

    @Autowired
    private EntityManager entityManager;

    private UsuarioEntity cachedUser;
    private AlumnoEntity cachedStudent;
    private ChatSesionEntity cachedSession;

    @Test
    void startForStudent_ShouldCreateOpenSession_WhenStudentExists() {
        AlumnoEntity student = givenStudentEntity();
        LocalDateTime now = LocalDateTime.now().truncatedTo(MILLIS);

        ChatSession session = whenStartingSessionForStudent(student, now);

        thenSessionIsCreatedCorrectly(session, student);
    }

    @Test
    void findOpenByIdAndStudentId_ShouldMapEntity_WhenExists() {
        AlumnoEntity student = givenStudentEntity();
        ChatSesionEntity sessionEntity = givenSessionEntity();

        Optional<ChatSession> result = whenFindingOpenSessionByStudent(sessionEntity.getId(), student.getId());

        thenSessionIsReturned(result, sessionEntity);
    }

    @Test
    void findOpenByIdAndUserId_ShouldMapEntity_WhenExists() {
        UsuarioEntity user = givenUserEntity();
        ChatSesionEntity sessionEntity = givenSessionEntity();

        Optional<ChatSession> result = whenFindingOpenSessionByUser(sessionEntity.getId(), user.getId());

        thenSessionIsReturned(result, sessionEntity);
    }

    @Test
    void findById_ShouldMapEntities_WhenExist() {
        AlumnoEntity student = givenStudentEntity();
        UsuarioEntity user = givenUserEntity();
        ChatSesionEntity sessionEntity = givenClosedSessionEntity();

        Optional<ChatSession> resultByStudent = whenFindingSessionByStudent(sessionEntity.getId(), student.getId());
        Optional<ChatSession> resultByUser = whenFindingSessionByUser(sessionEntity.getId(), user.getId());

        thenClosedSessionsAreMappedProperly(resultByStudent, resultByUser);
    }

    @Test
    void findByStudentIdAndUserId_ShouldReturnPagedResults() {
        AlumnoEntity student = givenStudentEntity();
        UsuarioEntity user = givenUserEntity();
        givenSessionEntity();

        List<ChatSession> studentSessions = whenFindingSessionsByStudentPaged(student.getId(), 0, 5);
        List<ChatSession> userSessions = whenFindingSessionsByUserPaged(user.getId(), 0, 10);

        thenPagedSessionsAreReturned(studentSessions, userSessions);
    }

    @Test
    void startForStudent_ShouldFail_WhenStudentDoesNotExist() {
        UUID randomStudentId = UUID.randomUUID();

        assertThrows(AlumnoNoEncontradoException.class,
                () -> repository.startForStudent(randomStudentId, LocalDateTime.now()));
    }

    @Test
    void updateLastActivity_ShouldUpdateDate_WhenSessionExists() {
        ChatSesionEntity sessionEntity = givenSessionEntity();
        LocalDateTime newActivity = LocalDateTime.now().plusHours(1).truncatedTo(MILLIS);

        whenUpdatingLastActivity(sessionEntity.getId(), newActivity);

        thenActivityDateIsUpdated(sessionEntity.getId(), newActivity);
    }

    @Test
    void close_ShouldUpdateStatus_WhenSessionExists() {
        ChatSesionEntity sessionEntity = givenSessionEntity();
        LocalDateTime closedAt = LocalDateTime.now().truncatedTo(MILLIS);

        whenClosingSession(sessionEntity.getId(), closedAt);

        thenSessionIsClosed(sessionEntity.getId(), closedAt);
    }

    @Test
    void deleteById_ShouldRemoveEntity_WhenExists() {
        ChatSesionEntity sessionToDelete = givenPersistedSessionToDelete();

        whenDeletingSession(sessionToDelete.getId());

        thenSessionIsDeleted(sessionToDelete.getId());
    }

    private UsuarioEntity givenUserEntity() {
        return generateUserEntity();
    }

    private AlumnoEntity givenStudentEntity() {
        return generateStudentEntity();
    }

    private ChatSesionEntity givenSessionEntity() {
        return generateSessionEntity();
    }

    private ChatSesionEntity givenClosedSessionEntity() {
        ChatSesionEntity sessionEntity = generateSessionEntity();
        sessionEntity.setEstado(EstadoChatSesion.CERRADA);
        entityManager.merge(sessionEntity);
        entityManager.flush();
        return sessionEntity;
    }

    private ChatSesionEntity givenPersistedSessionToDelete() {
        ChatSesionEntity sessionToDelete = new ChatSesionEntity();
        sessionToDelete.setUsuarioEntity(generateUserEntity());
        sessionToDelete.setEstado(EstadoChatSesion.ABIERTA);
        entityManager.persist(sessionToDelete);
        entityManager.flush();
        return sessionToDelete;
    }

    private ChatSession whenStartingSessionForStudent(AlumnoEntity student, LocalDateTime timestamp) {
        return repository.startForStudent(student.getId(), timestamp);
    }

    private Optional<ChatSession> whenFindingOpenSessionByStudent(UUID sessionId, UUID studentId) {
        return repository.findOpenByIdAndStudentId(sessionId, studentId);
    }

    private Optional<ChatSession> whenFindingOpenSessionByUser(UUID sessionId, UUID userId) {
        return repository.findOpenByIdAndUserId(sessionId, userId);
    }

    private Optional<ChatSession> whenFindingSessionByStudent(UUID sessionId, UUID studentId) {
        return repository.findByIdAndStudentId(sessionId, studentId);
    }

    private Optional<ChatSession> whenFindingSessionByUser(UUID sessionId, UUID userId) {
        return repository.findByIdAndUserId(sessionId, userId);
    }

    private List<ChatSession> whenFindingSessionsByStudentPaged(UUID studentId, int offset, int limit) {
        return repository.findByStudentId(studentId, offset, limit);
    }

    private List<ChatSession> whenFindingSessionsByUserPaged(UUID userId, int offset, int limit) {
        return repository.findByUserId(userId, offset, limit);
    }

    private void whenUpdatingLastActivity(UUID sessionId, LocalDateTime timestamp) {
        repository.updateLastActivity(sessionId, timestamp);
        entityManager.flush();
        entityManager.clear();
    }

    private void whenClosingSession(UUID sessionId, LocalDateTime closedAt) {
        repository.close(sessionId, closedAt);
        entityManager.flush();
        entityManager.clear();
    }

    private void whenDeletingSession(UUID sessionId) {
        repository.deleteById(sessionId);
    }

    private void thenSessionIsCreatedCorrectly(ChatSession session, AlumnoEntity student) {
        Optional<ChatSesionEntity> savedOpt = chatSessionRepository.findById(session.id());
        assertTrue(savedOpt.isPresent());
        ChatSesionEntity savedEntity = savedOpt.get();
        assertEquals(EstadoChatSesion.ABIERTA, savedEntity.getEstado());
        assertEquals(student.getUsuario().getId(), savedEntity.getUsuarioEntity().getId());
    }

    private void thenSessionIsReturned(Optional<ChatSession> result, ChatSesionEntity sessionEntity) {
        assertTrue(result.isPresent());
        assertEquals(sessionEntity.getId(), result.get().id());
    }

    private void thenClosedSessionsAreMappedProperly(Optional<ChatSession> resultByStudent, Optional<ChatSession> resultByUser) {
        assertTrue(resultByStudent.isPresent());
        assertEquals(EstadoChatSesion.CERRADA, resultByStudent.get().status());
        assertTrue(resultByUser.isPresent());
        assertEquals(EstadoChatSesion.CERRADA, resultByUser.get().status());
    }

    private void thenPagedSessionsAreReturned(List<ChatSession> studentSessions, List<ChatSession> userSessions) {
        assertFalse(studentSessions.isEmpty());
        assertFalse(userSessions.isEmpty());
    }

    private void thenActivityDateIsUpdated(UUID sessionId, LocalDateTime newActivity) {
        Optional<ChatSesionEntity> updatedOpt = chatSessionRepository.findById(sessionId);
        assertTrue(updatedOpt.isPresent());
        assertEquals(newActivity, updatedOpt.get().getFechaUltimaActividad());
    }

    private void thenSessionIsClosed(UUID sessionId, LocalDateTime closedAt) {
        Optional<ChatSesionEntity> updatedOpt = chatSessionRepository.findById(sessionId);
        assertTrue(updatedOpt.isPresent());
        assertEquals(EstadoChatSesion.CERRADA, updatedOpt.get().getEstado());
        assertEquals(closedAt, updatedOpt.get().getFechaUltimaActividad());
    }

    private void thenSessionIsDeleted(UUID sessionId) {
        assertTrue(chatSessionRepository.findById(sessionId).isEmpty());
    }

    private UsuarioEntity generateUserEntity() {
        if (this.cachedUser == null) {
            UsuarioEntity user = new UsuarioEntity();
            user.setIdCognito("cognito-chat-session");
            user.setNombre("John");
            user.setApellido("Doe");
            user.setEmail("john.doe@chat.com");
            user.setRol(RolUsuario.ALUMNO);
            user.setAutenticadoConCognito(false);
            user.setActivo(true);
            user.setPlan(TipoPlan.GRATUITO);
            entityManager.persist(user);
            entityManager.flush();
            this.cachedUser = user;
        }
        return this.cachedUser;
    }

    private AlumnoEntity generateStudentEntity() {
        if (this.cachedStudent == null) {
            UsuarioEntity user = generateUserEntity();
            AlumnoEntity student = new AlumnoEntity();
            student.setUsuario(user);
            student.setActivo(true);
            entityManager.persist(student);
            entityManager.flush();
            this.cachedStudent = student;
        }
        return this.cachedStudent;
    }

    private ChatSesionEntity generateSessionEntity() {
        if (this.cachedSession == null) {
            UsuarioEntity user = generateUserEntity();
            ChatSesionEntity session = new ChatSesionEntity();
            session.setUsuarioEntity(user);
            session.setEstado(EstadoChatSesion.ABIERTA);
            session.setFechaInicio(LocalDateTime.now().minusHours(1));
            session.setFechaUltimaActividad(LocalDateTime.now().minusMinutes(30));
            entityManager.persist(session);
            entityManager.flush();
            this.cachedSession = session;
        }
        return this.cachedSession;
    }
}
