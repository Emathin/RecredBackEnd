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

import static org.junit.jupiter.api.Assertions.*;

@Transactional
class ChatSessionPersistenceRepositoryTest extends AbstractIntegrationTest {

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
        AlumnoEntity student = generateStudentEntity();
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);

        ChatSession session = repository.startForStudent(student.getId(), now);

        Optional<ChatSesionEntity> savedOpt = chatSessionRepository.findById(session.id());
        assertTrue(savedOpt.isPresent());
        ChatSesionEntity savedEntity = savedOpt.get();
        assertEquals(EstadoChatSesion.ABIERTA, savedEntity.getEstado());
        assertEquals(student.getUsuario().getId(), savedEntity.getUsuarioEntity().getId());
    }

    @Test
    void findOpenByIdAndStudentId_ShouldMapEntity_WhenExists() {
        AlumnoEntity student = generateStudentEntity();
        ChatSesionEntity sessionEntity = generateSessionEntity();

        Optional<ChatSession> result = repository.findOpenByIdAndStudentId(sessionEntity.getId(), student.getId());

        assertTrue(result.isPresent());
        assertEquals(sessionEntity.getId(), result.get().id());
    }

    @Test
    void findOpenByIdAndUserId_ShouldMapEntity_WhenExists() {
        UsuarioEntity user = generateUserEntity();
        ChatSesionEntity sessionEntity = generateSessionEntity();

        Optional<ChatSession> result = repository.findOpenByIdAndUserId(sessionEntity.getId(), user.getId());

        assertTrue(result.isPresent());
        assertEquals(sessionEntity.getId(), result.get().id());
    }

    @Test
    void findById_ShouldMapEntities_WhenExist() {
        AlumnoEntity student = generateStudentEntity();
        UsuarioEntity user = generateUserEntity();
        ChatSesionEntity sessionEntity = generateSessionEntity();

        sessionEntity.setEstado(EstadoChatSesion.CERRADA);
        entityManager.merge(sessionEntity);
        entityManager.flush();

        Optional<ChatSession> resultByStudent = repository.findByIdAndStudentId(sessionEntity.getId(), student.getId());
        Optional<ChatSession> resultByUser = repository.findByIdAndUserId(sessionEntity.getId(), user.getId());

        assertTrue(resultByStudent.isPresent());
        assertEquals(EstadoChatSesion.CERRADA, resultByStudent.get().status());
        assertTrue(resultByUser.isPresent());
        assertEquals(EstadoChatSesion.CERRADA, resultByUser.get().status());
    }

    @Test
    void findByStudentIdAndUserId_ShouldReturnPagedResults() {
        AlumnoEntity student = generateStudentEntity();
        UsuarioEntity user = generateUserEntity();
        generateSessionEntity();

        List<ChatSession> studentSessions = repository.findByStudentId(student.getId(), 0, 5);
        List<ChatSession> userSessions = repository.findByUserId(user.getId(), 0, 10);

        assertFalse(studentSessions.isEmpty());
        assertFalse(userSessions.isEmpty());
    }

    @Test
    void startForStudent_ShouldFail_WhenStudentDoesNotExist() {
        UUID randomStudentId = UUID.randomUUID();

        assertThrows(AlumnoNoEncontradoException.class,
                () -> repository.startForStudent(randomStudentId, LocalDateTime.now()));
    }

    @Test
    void updateLastActivity_ShouldUpdateDate_WhenSessionExists() {
        ChatSesionEntity sessionEntity = generateSessionEntity();
        LocalDateTime newActivity = LocalDateTime.now().plusHours(1).truncatedTo(ChronoUnit.MILLIS);

        repository.updateLastActivity(sessionEntity.getId(), newActivity);
        entityManager.flush();
        entityManager.clear();

        Optional<ChatSesionEntity> updatedOpt = chatSessionRepository.findById(sessionEntity.getId());
        assertTrue(updatedOpt.isPresent());
        assertEquals(newActivity, updatedOpt.get().getFechaUltimaActividad());
    }

    @Test
    void close_ShouldUpdateStatus_WhenSessionExists() {
        ChatSesionEntity sessionEntity = generateSessionEntity();
        LocalDateTime closedAt = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);

        repository.close(sessionEntity.getId(), closedAt);
        entityManager.flush();
        entityManager.clear();

        Optional<ChatSesionEntity> updatedOpt = chatSessionRepository.findById(sessionEntity.getId());
        assertTrue(updatedOpt.isPresent());
        assertEquals(EstadoChatSesion.CERRADA, updatedOpt.get().getEstado());
        assertEquals(closedAt, updatedOpt.get().getFechaUltimaActividad());
    }

    @Test
    void deleteById_ShouldRemoveEntity_WhenExists() {
        ChatSesionEntity sessionToDelete = new ChatSesionEntity();
        sessionToDelete.setUsuarioEntity(generateUserEntity());
        sessionToDelete.setEstado(EstadoChatSesion.ABIERTA);
        entityManager.persist(sessionToDelete);
        entityManager.flush();

        repository.deleteById(sessionToDelete.getId());

        assertTrue(chatSessionRepository.findById(sessionToDelete.getId()).isEmpty());
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
