package com.recred.backend.integration.persistence;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Buffet;
import com.recred.backend.domain.models.Colegio;
import com.recred.backend.domain.models.Kiosquero;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.models.TipoPlan;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.infrastructure.entities.AlumnoEntity;
import com.recred.backend.infrastructure.entities.BuffetEntity;
import com.recred.backend.infrastructure.entities.ColegioEntity;
import com.recred.backend.infrastructure.entities.KiosqueroEntity;
import com.recred.backend.infrastructure.entities.TutorEntity;
import com.recred.backend.infrastructure.entities.UsuarioEntity;
import com.recred.backend.infrastructure.persistence.SpringDataColegioRepository;
import com.recred.backend.infrastructure.persistence.SpringDataUsuarioRepository;
import com.recred.backend.infrastructure.persistence.UsuarioRepositoryImpl;
import com.recred.backend.integration.AbstractIntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Transactional
class UsuarioRepositoryImplIT extends AbstractIntegrationTest {

    @Autowired
    private UsuarioRepositoryImpl repository;

    @Autowired
    private SpringDataUsuarioRepository springDataUsuarioRepository;

    @Autowired
    private SpringDataColegioRepository springDataColegioRepository;

    @Autowired
    private EntityManager entityManager;

    private UsuarioEntity cachedUser;
    private ColegioEntity cachedColegio;
    private AlumnoEntity cachedAlumno;
    private TutorEntity cachedTutor;
    private KiosqueroEntity cachedKiosquero;
    private Usuario cachedUsuarioCommand;

    @Test
    void saveShouldMapDomainUserWithRelationsToEntityAndBack() {
        givenContextForMappingDomainUser();
        
        Usuario result = whenSavingUsuario();

        thenUsuarioRelationsWereSaved(result);
    }

    @Test
    void findByIdShouldMapEntityWhenPresent() {
        givenSavedUserEntity();

        Optional<Usuario> result = whenFindingUsuarioById();

        thenUsuarioMatchesEntity(result);
    }

    @Test
    void findByIdCognitoShouldMapEntityWhenPresent() {
        givenSavedUserEntity();

        Optional<Usuario> result = whenFindingUsuarioByIdCognito();

        thenUsuarioMatchesEntity(result);
    }

    @Test
    void findByEmailShouldMapEntityWhenPresent() {
        givenSavedUserEntity();

        Optional<Usuario> result = whenFindingUsuarioByEmail();

        thenUsuarioMatchesEntity(result);
    }

    @Test
    void updateProfileShouldNotPersistKiosqueroWithoutCuit() {
        givenKiosqueroUserContext();

        whenUpdatingProfile();

        thenProfileWasUpdatedAndKiosqueroPreserved();
    }

    @Test
    void updatePlanShouldPreserveTutorStudents() {
        givenTutorUserWithStudent();

        whenUpdatingPlan();

        thenPlanWasUpdatedAndStudentPreserved();
    }

    private void givenContextForMappingDomainUser() {
        this.cachedColegio = generateColegioEntity();
        this.cachedAlumno = generateStudentEntity();
        this.cachedTutor = generateTutorEntity();
        this.cachedKiosquero = generateKiosqueroEntityWithBuffet(this.cachedColegio);
        
        UsuarioEntity baseUser = userEntity();
        springDataUsuarioRepository.save(baseUser);
        
        entityManager.flush();
        entityManager.clear();

        this.cachedUsuarioCommand = createUsuarioDomainWithRelations(baseUser.getId(), this.cachedColegio, this.cachedAlumno, this.cachedTutor, this.cachedKiosquero);
    }

    private void givenSavedUserEntity() {
        this.cachedUser = userEntity();
        springDataUsuarioRepository.save(this.cachedUser);
        entityManager.flush();
        entityManager.clear();
    }

    private void givenKiosqueroUserContext() {
        ColegioEntity colegioEntity = generateColegioEntity();
        this.cachedKiosquero = generateKiosqueroEntityWithBuffet(colegioEntity);
        entityManager.flush();
        entityManager.clear();
    }

    private void givenTutorUserWithStudent() {
        this.cachedTutor = generateTutorEntityWithStudent();
        entityManager.flush();
        entityManager.clear();
    }

    private Usuario whenSavingUsuario() {
        Usuario result = repository.save(this.cachedUsuarioCommand);
        entityManager.flush();
        entityManager.clear();
        return result;
    }

    private Optional<Usuario> whenFindingUsuarioById() {
        return repository.findById(this.cachedUser.getId());
    }

    private Optional<Usuario> whenFindingUsuarioByIdCognito() {
        return repository.findByIdCognito(this.cachedUser.getIdCognito());
    }

    private Optional<Usuario> whenFindingUsuarioByEmail() {
        return repository.findByEmail(this.cachedUser.getEmail());
    }

    private void whenUpdatingProfile() {
        repository.updateProfile(this.cachedKiosquero.getUsuario().getIdCognito(), "Nombre", "Actualizado", "5491161718479", "45238796", java.time.LocalDateTime.now());
        entityManager.flush();
        entityManager.clear();
    }

    private void whenUpdatingPlan() {
        repository.updatePlan(this.cachedTutor.getUsuario().getId(), TipoPlan.INTERMEDIO);
        entityManager.flush();
        entityManager.clear();
    }

    private void thenUsuarioMatchesEntity(Optional<Usuario> result) {
        assertTrue(result.isPresent());
        assertEquals(this.cachedUser.getId(), result.get().getId());
    }

    private void thenUsuarioRelationsWereSaved(Usuario result) {
        UsuarioEntity saved = springDataUsuarioRepository.findById(result.getId()).orElseThrow();
        assertEquals(result.getId(), saved.getId());
        assertEquals(this.cachedUsuarioCommand.getIdCognito(), saved.getIdCognito());
        assertEquals(this.cachedColegio.getId(), saved.getColegioEntity().getId());
        assertEquals(this.cachedAlumno.getId(), saved.getAlumnoEntity().getId());
        assertEquals(this.cachedTutor.getId(), saved.getTutor().getId());
        assertEquals(this.cachedKiosquero.getId(), saved.getKiosquero().getId());
    }

    private void thenProfileWasUpdatedAndKiosqueroPreserved() {
        UsuarioEntity saved = springDataUsuarioRepository.findById(this.cachedKiosquero.getUsuario().getId()).orElseThrow();
        assertEquals("Nombre", saved.getNombre());
        assertEquals("Actualizado", saved.getApellido());
        assertEquals("5491161718479", saved.getTelefono());
        assertEquals(this.cachedKiosquero.getCuit(), saved.getKiosquero().getCuit());
    }

    private void thenPlanWasUpdatedAndStudentPreserved() {
        UsuarioEntity saved = springDataUsuarioRepository.findById(this.cachedTutor.getUsuario().getId()).orElseThrow();
        assertEquals(TipoPlan.INTERMEDIO, saved.getPlan());
        UUID expectedStudentId = this.cachedTutor.getHijos().iterator().next().getId();
        assertTrue(saved.getTutor().getHijos().stream()
                .anyMatch(student -> student.getId().equals(expectedStudentId)));
    }

    private Usuario user() {
        Usuario usuario = new Usuario();
        usuario.setIdCognito("sub-domain-" + UUID.randomUUID().toString().substring(0, 8));
        usuario.setNombre("Juan");
        usuario.setApellido("Perez");
        usuario.setEmail("user-" + UUID.randomUUID().toString().substring(0, 5) + "@example.com");
        usuario.setTelefono("112233");
        usuario.setDni("12345678");
        usuario.setRol(RolUsuario.PADRE);
        usuario.setAutenticadoConCognito(true);
        usuario.setActivo(true);
        return usuario;
    }

    private UsuarioEntity userEntity() {
        UsuarioEntity entity = new UsuarioEntity();
        entity.setIdCognito("sub-entity-" + UUID.randomUUID().toString().substring(0, 8));
        entity.setNombre("Juan");
        entity.setApellido("Perez");
        entity.setEmail("user-" + UUID.randomUUID().toString().substring(0, 5) + "@example.com");
        entity.setTelefono("112233");
        entity.setDni("12345678");
        entity.setRol(RolUsuario.PADRE);
        entity.setAutenticadoConCognito(true);
        entity.setActivo(true);
        return entity;
    }

    private ColegioEntity generateColegioEntity() {
        ColegioEntity colegioEntity = new ColegioEntity();
        colegioEntity.setNombre("Test School");
        colegioEntity.setCue("12345");
        colegioEntity.setActivo(true);
        return springDataColegioRepository.save(colegioEntity);
    }

    private AlumnoEntity generateStudentEntity() {
        UsuarioEntity studentUser = new UsuarioEntity();
        studentUser.setIdCognito("sub-student-" + UUID.randomUUID());
        studentUser.setNombre("Student");
        studentUser.setApellido("Test");
        studentUser.setEmail("student@test.com");
        studentUser.setRol(RolUsuario.ALUMNO);
        studentUser.setActivo(true);
        entityManager.persist(studentUser);

        AlumnoEntity alumnoEntity = new AlumnoEntity();
        alumnoEntity.setUsuario(studentUser);
        alumnoEntity.setActivo(true);
        entityManager.persist(alumnoEntity);
        return alumnoEntity;
    }

    private TutorEntity generateTutorEntity() {
        UsuarioEntity tutorUser = new UsuarioEntity();
        tutorUser.setIdCognito("sub-tutor-" + UUID.randomUUID());
        tutorUser.setNombre("Tutor");
        tutorUser.setApellido("Test");
        tutorUser.setEmail("tutor@test.com");
        tutorUser.setRol(RolUsuario.PADRE);
        tutorUser.setActivo(true);
        entityManager.persist(tutorUser);

        TutorEntity tutorEntity = new TutorEntity();
        tutorEntity.setUsuario(tutorUser);
        tutorEntity.setActiva(true);
        entityManager.persist(tutorEntity);
        return tutorEntity;
    }

    private TutorEntity generateTutorEntityWithStudent() {
        TutorEntity tutorEntity = generateTutorEntity();
        AlumnoEntity alumnoEntity = generateStudentEntity();
        tutorEntity.addHijo(alumnoEntity);
        return tutorEntity;
    }

    private KiosqueroEntity generateKiosqueroEntityWithBuffet(ColegioEntity colegioEntity) {
        BuffetEntity buffetEntity = new BuffetEntity();
        buffetEntity.setNombre("Test Buffet");
        buffetEntity.setColegioEntity(colegioEntity);
        buffetEntity.setActivo(true);
        entityManager.persist(buffetEntity);

        UsuarioEntity kiosqueroUser = new UsuarioEntity();
        kiosqueroUser.setIdCognito("sub-kiosquero-" + UUID.randomUUID());
        kiosqueroUser.setNombre("Kiosquero");
        kiosqueroUser.setApellido("Test");
        kiosqueroUser.setEmail("kiosquero@test.com");
        kiosqueroUser.setRol(RolUsuario.VENDEDOR);
        kiosqueroUser.setActivo(true);
        entityManager.persist(kiosqueroUser);

        KiosqueroEntity kiosqueroEntity = new KiosqueroEntity();
        kiosqueroEntity.setUsuario(kiosqueroUser);
        kiosqueroEntity.setCuit("123456789");
        kiosqueroEntity.setBuffetEntity(buffetEntity);
        entityManager.persist(kiosqueroEntity);
        
        buffetEntity.setVendedor(kiosqueroEntity);
        entityManager.merge(buffetEntity);
        return kiosqueroEntity;
    }

    private Usuario createUsuarioDomainWithRelations(UUID baseUserId, ColegioEntity colegioEntity, AlumnoEntity alumnoEntity, TutorEntity tutorEntity, KiosqueroEntity kiosqueroEntity) {
        Usuario usuario = user();
        usuario.setId(baseUserId);
        
        Colegio colegio = new Colegio();
        colegio.setId(colegioEntity.getId());
        usuario.setColegio(colegio);
        
        Alumno alumno = new Alumno();
        alumno.setId(alumnoEntity.getId());
        usuario.setAlumno(alumno);
        
        Tutor tutor = new Tutor();
        tutor.setId(tutorEntity.getId());
        usuario.setTutor(tutor);
        
        Kiosquero kiosquero = new Kiosquero();
        kiosquero.setId(kiosqueroEntity.getId());
        kiosquero.setCuit(kiosqueroEntity.getCuit());
        Buffet buffet = new Buffet();
        buffet.setId(kiosqueroEntity.getBuffetEntity().getId());
        kiosquero.setBuffet(buffet);
        usuario.setKiosquero(kiosquero);
        
        return usuario;
    }
}
