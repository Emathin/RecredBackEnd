package com.recred.backend.integration.persistence;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.infrastructure.entities.AlumnoEntity;
import com.recred.backend.infrastructure.entities.ColegioEntity;
import com.recred.backend.infrastructure.entities.GradoEntity;
import com.recred.backend.infrastructure.entities.NivelEntity;
import com.recred.backend.infrastructure.entities.TutorEntity;
import com.recred.backend.infrastructure.entities.UsuarioEntity;
import com.recred.backend.infrastructure.persistence.SpringDataAlumnoRepository;
import com.recred.backend.infrastructure.persistence.SpringDataTutorRepository;
import com.recred.backend.infrastructure.persistence.TutorRepositoryImpl;
import com.recred.backend.integration.AbstractIntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

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

    private TutorEntity cachedTutor;
    private AlumnoEntity cachedChild;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    void findMethods_ShouldMapEntitiesToDomain() {
        givenTutorWithChild();

        thenTutorFindMethodsReturnCorrectDomainObject();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void findByAlumnoId_WhenCallerHasNoTransaction_ShouldMapGradeLevel() {
        givenStudentWithTutorAndGradeLevel();

        Tutor tutor = whenFindingTutorByStudentId();

        thenGradeLevelIsMappedCorrectly(tutor);
    }

    @Test
    void save_WhenTutorDoesNotExist_ShouldFail() {
        Tutor tutor = givenNonExistentTutor();

        assertThrows(IllegalArgumentException.class, () -> whenSavingTutor(tutor));
    }

    @Test
    void save_ShouldReplaceChildrenAndIgnoreChildWithoutId() {
        TutorUpdateContext ctx = givenTutorAndNewChildForUpdate();

        Tutor result = whenSavingTutor(ctx.command());

        thenTutorChildrenWereReplaced(result.getId(), ctx.newChild().getId());
    }

    private void givenTutorWithChild() {
        this.cachedChild = generateStudent();
        this.cachedTutor = generateTutorWithChild(this.cachedChild);
    }

    private Tutor givenNonExistentTutor() {
        Tutor tutor = new Tutor();
        tutor.setId(UUID.randomUUID());
        return tutor;
    }

    private TutorUpdateContext givenTutorAndNewChildForUpdate() {
        AlumnoEntity oldChild = generateStudent();
        TutorEntity existingTutor = generateTutorWithChild(oldChild);
        AlumnoEntity newChild = generateStudent();
        
        Tutor tutorUpdateCommand = createTutorDomainWithChildren(existingTutor.getId(), newChild.getId());
        return new TutorUpdateContext(existingTutor, newChild, tutorUpdateCommand);
    }

    private Tutor whenSavingTutor(Tutor tutor) {
        Tutor result = repository.save(tutor);
        entityManager.flush();
        entityManager.clear();
        return result;
    }

    private void thenTutorChildrenWereReplaced(UUID tutorId, UUID expectedChildId) {
        TutorEntity updated = springDataTutorRepository.findById(tutorId).orElseThrow();
        assertEquals(1, updated.getHijos().size());
        assertTrue(updated.getHijos().stream().anyMatch(h -> h.getId().equals(expectedChildId)));
    }

    private void thenTutorFindMethodsReturnCorrectDomainObject() {
        UUID tutorId = this.cachedTutor.getId();
        UUID userId = this.cachedTutor.getUsuario().getId();
        String idCognito = this.cachedTutor.getUsuario().getIdCognito();
        UUID studentId = this.cachedChild.getId();

        assertEquals(tutorId, repository.findById(tutorId).orElseThrow().getId());
        assertEquals(tutorId, repository.findByUsuarioId(userId).orElseThrow().getId());
        assertEquals(tutorId, repository.findByUsuarioIdCognito(idCognito).orElseThrow().getId());
        Tutor tutorByStudent = repository.findByAlumnoId(studentId).get(0);
        assertEquals(tutorId, tutorByStudent.getId());
        assertEquals(userId, tutorByStudent.getUsuario().getId());
        assertFalse(repository.findAll().isEmpty());
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
        return generateStudent(null);
    }

    private AlumnoEntity generateStudent(GradoEntity grade) {
        AlumnoEntity student = new AlumnoEntity();
        student.setUsuario(generateUser(RolUsuario.ALUMNO));
        student.setActivo(true);
        if (grade != null)
            student.setGradoEntity(grade);
        return springDataAlumnoRepository.save(student);
    }

    private String cachedLevelDescription;
    private UUID cachedStudentId;

    private void givenStudentWithTutorAndGradeLevel() {
        this.cachedLevelDescription = "Nivel " + UUID.randomUUID();
        this.cachedStudentId = createStudentWithTutorAndGradeLevel(this.cachedLevelDescription);
    }

    private Tutor whenFindingTutorByStudentId() {
        return repository.findByAlumnoId(this.cachedStudentId).get(0);
    }

    private void thenGradeLevelIsMappedCorrectly(Tutor tutor) {
        Alumno child = tutor.getHijos().iterator().next();
        assertEquals(this.cachedLevelDescription, child.getGrado().getNivel().getDescripcion());
    }

    private UUID createStudentWithTutorAndGradeLevel(String levelDescription) {
        return transactionTemplate.execute(status -> {
            NivelEntity level = generateLevel(levelDescription);
            ColegioEntity school = generateSchool(level);
            GradoEntity grade = generateGrade(school, level);
            AlumnoEntity student = generateStudent(grade);
            generateTutorWithChild(student);
            return student.getId();
        });
    }

    private NivelEntity generateLevel(String description) {
        NivelEntity level = new NivelEntity();
        level.setDescripcion(description);
        level.setActivo(true);
        entityManager.persist(level);
        return level;
    }

    private ColegioEntity generateSchool(NivelEntity level) {
        ColegioEntity school = new ColegioEntity();
        school.setNombre("Colegio " + UUID.randomUUID());
        school.setCue("CUE-" + UUID.randomUUID());
        school.setNivelEntity(level);
        school.setActivo(true);
        entityManager.persist(school);
        return school;
    }

    private GradoEntity generateGrade(ColegioEntity school, NivelEntity level) {
        GradoEntity grade = new GradoEntity();
        grade.setColegioEntity(school);
        grade.setNivelEntity(level);
        grade.setAnio("1");
        grade.setDivision("A");
        grade.setActivo(true);
        entityManager.persist(grade);
        return grade;
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

    private record TutorUpdateContext(TutorEntity existingTutor, AlumnoEntity newChild, Tutor command) {}
}
