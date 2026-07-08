package com.recred.backend.integration.persistence;

import com.recred.backend.domain.models.ConsumptionAlertCandidate;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.infrastructure.entities.AlumnoEntity;
import com.recred.backend.infrastructure.entities.TutorEntity;
import com.recred.backend.infrastructure.entities.UsuarioEntity;
import com.recred.backend.infrastructure.persistence.ConsumptionAlertStudentPersistenceRepository;
import com.recred.backend.integration.AbstractIntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Transactional
class ConsumptionAlertStudentPersistenceRepositoryIT extends AbstractIntegrationTest {

    @Autowired
    private ConsumptionAlertStudentPersistenceRepository repository;

    @Autowired
    private EntityManager entityManager;

    private UsuarioEntity cachedStudentUser;
    private AlumnoEntity cachedStudent;

    @Test
    void findActiveCandidates_ShouldMapActiveStudentsWithActiveTutors() {
        AlumnoEntity student = generateStudentEntity("John", "Doe");
        
        TutorEntity activeTutor = generateTutorEntity("Anna", "Doe", true, true, "cognito-tutor-active");
        TutorEntity inactiveTutor = generateTutorEntity("Louis", "Doe", false, true, "cognito-tutor-inactive");
        
        student.getTutores().add(activeTutor);
        student.getTutores().add(inactiveTutor);
        
        activeTutor.getHijos().add(student);
        inactiveTutor.getHijos().add(student);
        
        entityManager.merge(student);
        entityManager.merge(activeTutor);
        entityManager.merge(inactiveTutor);
        entityManager.flush();
        entityManager.clear();

        List<ConsumptionAlertCandidate> candidates = repository.findActiveCandidates();

        assertFalse(candidates.isEmpty());
        ConsumptionAlertCandidate candidate = candidates.stream().filter(c -> c.studentId().equals(student.getId())).findFirst().orElseThrow();
        assertEquals(student.getUsuario().getId(), candidate.studentUserId());
        assertEquals("John Doe", candidate.studentName());
        assertTrue(candidate.tutors().stream().anyMatch(t -> "Anna Doe".equals(t.name())));
    }

    @Test
    void findActiveCandidates_ShouldUseDefaultNameWhenStudentUserNameIsBlank() {
        AlumnoEntity student = generateStudentEntity(" ", " ");
        TutorEntity activeTutor = generateTutorEntity("Anna", "Doe", true, true, "cognito-tutor-active-2");
        student.getTutores().add(activeTutor);
        activeTutor.getHijos().add(student);
        
        entityManager.merge(student);
        entityManager.merge(activeTutor);
        entityManager.flush();
        entityManager.clear();

        List<ConsumptionAlertCandidate> candidates = repository.findActiveCandidates();

        assertFalse(candidates.isEmpty());
        ConsumptionAlertCandidate candidate = candidates.stream().filter(c -> c.studentId().equals(student.getId())).findFirst().orElseThrow();
        assertEquals("el alumno", candidate.studentName());
    }

    private UsuarioEntity generateStudentUserEntity(String firstName, String lastName) {
        if (this.cachedStudentUser == null) {
            UsuarioEntity user = new UsuarioEntity();
            user.setIdCognito("cognito-student-user-candidates");
            user.setNombre(firstName);
            user.setApellido(lastName);
            user.setEmail("student.candidate@test.com");
            user.setRol(RolUsuario.ALUMNO);
            user.setAutenticadoConCognito(false);
            user.setActivo(true);
            entityManager.persist(user);
            entityManager.flush();
            this.cachedStudentUser = user;
        }
        return this.cachedStudentUser;
    }

    private AlumnoEntity generateStudentEntity(String firstName, String lastName) {
        if (this.cachedStudent == null) {
            UsuarioEntity user = generateStudentUserEntity(firstName, lastName);
            AlumnoEntity student = new AlumnoEntity();
            student.setUsuario(user);
            student.setActivo(true);
            entityManager.persist(student);
            entityManager.flush();
            this.cachedStudent = student;
        }
        return this.cachedStudent;
    }

    private TutorEntity generateTutorEntity(String firstName, String lastName, boolean activeTutor, boolean activeUser, String cognitoId) {
        UsuarioEntity user = new UsuarioEntity();
        user.setIdCognito(cognitoId);
        user.setNombre(firstName);
        user.setApellido(lastName);
        user.setEmail(cognitoId + "@test.com");
        user.setRol(RolUsuario.PADRE);
        user.setAutenticadoConCognito(false);
        user.setActivo(activeUser);
        entityManager.persist(user);

        TutorEntity tutor = new TutorEntity();
        tutor.setUsuario(user);
        tutor.setActiva(activeTutor);
        entityManager.persist(tutor);
        
        return tutor;
    }
}
