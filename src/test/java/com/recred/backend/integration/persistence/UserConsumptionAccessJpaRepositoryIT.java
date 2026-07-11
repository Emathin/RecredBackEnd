package com.recred.backend.integration.persistence;

import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.infrastructure.entities.AlumnoEntity;
import com.recred.backend.infrastructure.entities.TutorEntity;
import com.recred.backend.infrastructure.entities.UsuarioEntity;
import com.recred.backend.infrastructure.persistence.SpringDataAlumnoRepository;
import com.recred.backend.infrastructure.persistence.SpringDataTutorRepository;
import com.recred.backend.infrastructure.persistence.UserConsumptionAccessJpaRepository;
import com.recred.backend.integration.AbstractIntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Transactional
class UserConsumptionAccessJpaRepositoryIT extends AbstractIntegrationTest {

    @Autowired
    private UserConsumptionAccessJpaRepository repository;

    @Autowired
    private SpringDataTutorRepository tutorRepository;

    @Autowired
    private SpringDataAlumnoRepository alumnoRepository;

    @Autowired
    private EntityManager entityManager;

    private TutorEntity cachedTutor;
    private AlumnoEntity cachedStudent;

    @Test
    void tutorCanAccessStudentWhenAssociationExists() {
        givenTutorAndStudentWithAssociation();

        boolean result = whenCheckingAccess();

        thenAccessIsGranted(result);
    }

    @Test
    void tutorCannotAccessStudentWhenAssociationDoesNotExist() {
        givenTutorAndStudentWithoutAssociation();

        boolean result = whenCheckingAccess();

        thenAccessIsDenied(result);
    }

    private void givenTutorAndStudentWithAssociation() {
        this.cachedTutor = generateTutor();
        this.cachedStudent = generateStudent();
        this.cachedTutor.addHijo(this.cachedStudent);
        tutorRepository.save(this.cachedTutor);
        entityManager.flush();
        entityManager.clear();
    }

    private void givenTutorAndStudentWithoutAssociation() {
        this.cachedTutor = generateTutor();
        this.cachedStudent = generateStudent();
        entityManager.flush();
        entityManager.clear();
    }

    private boolean whenCheckingAccess() {
        return repository.tutorCanAccessStudent(this.cachedTutor.getUsuario().getId(), this.cachedStudent.getId());
    }

    private void thenAccessIsGranted(boolean result) {
        assertTrue(result);
    }

    private void thenAccessIsDenied(boolean result) {
        assertFalse(result);
    }

    private UsuarioEntity generateUser(RolUsuario role) {
        UsuarioEntity user = new UsuarioEntity();
        user.setIdCognito("sub-" + UUID.randomUUID().toString().substring(0, 8));
        user.setNombre("Test");
        user.setApellido("User");
        user.setEmail("user" + UUID.randomUUID().toString().substring(0, 5) + "@test.com");
        user.setRol(role);
        user.setActivo(true);
        entityManager.persist(user);
        return user;
    }

    private TutorEntity generateTutor() {
        TutorEntity tutor = new TutorEntity();
        tutor.setUsuario(generateUser(RolUsuario.PADRE));
        tutor.setActiva(true);
        return tutorRepository.save(tutor);
    }

    private AlumnoEntity generateStudent() {
        AlumnoEntity student = new AlumnoEntity();
        student.setUsuario(generateUser(RolUsuario.ALUMNO));
        student.setActivo(true);
        return alumnoRepository.save(student);
    }
}
