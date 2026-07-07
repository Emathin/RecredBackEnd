package com.recred.backend.integration.persistence;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.infrastructure.entities.AlumnoEntity;
import com.recred.backend.infrastructure.entities.TutorEntity;
import com.recred.backend.infrastructure.entities.UsuarioEntity;
import com.recred.backend.infrastructure.persistence.SpringDataAlumnoRepository;
import com.recred.backend.infrastructure.persistence.SpringDataTutorRepository;
import com.recred.backend.infrastructure.persistence.TutorRepositoryImpl;
import com.recred.backend.integration.AbstractIntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Transactional
class TutorRepositoryImplIT extends AbstractIntegrationTest {

    @Autowired
    private TutorRepositoryImpl repository;

    @Autowired
    private SpringDataTutorRepository springDataTutorRepository;

    @Autowired
    private SpringDataAlumnoRepository springDataAlumnoRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void findMethods_ShouldMapEntitiesToDomain() {
        AlumnoEntity student = generateStudent();
        TutorEntity entity = generateTutorWithChild(student);
        UsuarioEntity user = entity.getUsuario();

        assertTutorFindMethodsReturnCorrectDomainObject(
                entity.getId(),
                user.getId(),
                user.getIdCognito(),
                student.getId()
        );
    }

    @Test
    void save_WhenTutorDoesNotExist_ShouldFail() {
        Tutor tutor = new Tutor();
        tutor.setId(UUID.randomUUID());

        assertThrows(IllegalArgumentException.class, () -> repository.save(tutor));
    }

    @Test
    void save_ShouldReplaceChildrenAndIgnoreChildWithoutId() {

        AlumnoEntity oldChild = generateStudent();
        TutorEntity existingTutor = generateTutorWithChild(oldChild);
        AlumnoEntity newChild = generateStudent();
        
        Tutor tutorUpdateCommand = createTutorDomainWithChildren(existingTutor.getId(), newChild.getId());

        Tutor result = repository.save(tutorUpdateCommand);
        entityManager.flush();
        entityManager.clear();

        assertEquals(existingTutor.getId(), result.getId());
        assertTutorChildrenWereReplaced(existingTutor.getId(), newChild.getId());
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

    private AlumnoEntity generateStudent() {
        AlumnoEntity student = new AlumnoEntity();
        student.setUsuario(generateUser(RolUsuario.ALUMNO));
        student.setActivo(true);
        return springDataAlumnoRepository.save(student);
    }

    private TutorEntity generateTutorWithChild(AlumnoEntity child) {
        TutorEntity tutor = new TutorEntity();
        tutor.setUsuario(generateUser(RolUsuario.PADRE));
        tutor.setActiva(true);
        tutor.addHijo(child);
        TutorEntity saved = springDataTutorRepository.save(tutor);
        entityManager.flush();
        entityManager.clear();
        return saved;
    }

    private Tutor createTutorDomainWithChildren(UUID tutorId, UUID newChildId) {
        Tutor tutor = new Tutor();
        tutor.setId(tutorId);
        
        Alumno childWithId = new Alumno();
        childWithId.setId(newChildId);
        
        Alumno childWithoutId = new Alumno();
        
        tutor.setHijos(Set.of(childWithId, childWithoutId));
        return tutor;
    }

    private void assertTutorChildrenWereReplaced(UUID tutorId, UUID expectedChildId) {
        TutorEntity updated = springDataTutorRepository.findById(tutorId).orElseThrow();
        assertEquals(1, updated.getHijos().size());
        assertTrue(updated.getHijos().stream().anyMatch(h -> h.getId().equals(expectedChildId)));
    }

    private void assertTutorFindMethodsReturnCorrectDomainObject(
            UUID tutorId,
            UUID userId,
            String idCognito,
            UUID studentId
    ) {
        assertEquals(tutorId, repository.findById(tutorId).orElseThrow().getId());
        assertEquals(tutorId, repository.findByUsuarioId(userId).orElseThrow().getId());
        assertEquals(tutorId, repository.findByUsuarioIdCognito(idCognito).orElseThrow().getId());
        Tutor tutorByStudent = repository.findByAlumnoId(studentId).get(0);
        assertEquals(tutorId, tutorByStudent.getId());
        assertEquals(userId, tutorByStudent.getUsuario().getId());
        assertFalse(repository.findAll().isEmpty());
    }
}
