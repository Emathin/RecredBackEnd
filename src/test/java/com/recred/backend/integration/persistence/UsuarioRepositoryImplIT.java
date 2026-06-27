package com.recred.backend.integration.persistence;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Buffet;
import com.recred.backend.domain.models.Colegio;
import com.recred.backend.domain.models.Kiosquero;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.Tutor;
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

    @Test
    void saveShouldMapDomainUserWithRelationsToEntityAndBack() {
        ColegioEntity colegioEntity = generateColegioEntity();
        AlumnoEntity alumnoEntity = generateStudentEntity();
        TutorEntity tutorEntity = generateTutorEntity();
        KiosqueroEntity kiosqueroEntity = generateKiosqueroEntityWithBuffet(colegioEntity);
        
        UsuarioEntity baseUser = userEntity();
        springDataUsuarioRepository.save(baseUser);
        
        entityManager.flush();
        entityManager.clear();

        Usuario usuarioCommand = createUsuarioDomainWithRelations(baseUser.getId(), colegioEntity, alumnoEntity, tutorEntity, kiosqueroEntity);

        Usuario result = repository.save(usuarioCommand);
        entityManager.flush();
        entityManager.clear();

        assertUsuarioRelationsWereSaved(usuarioCommand, result, colegioEntity, alumnoEntity, tutorEntity, kiosqueroEntity);
    }

    @Test
    void findByIdShouldMapEntityWhenPresent() {
        UsuarioEntity entity = userEntity();
        springDataUsuarioRepository.save(entity);
        entityManager.flush();
        entityManager.clear();

        Optional<Usuario> result = repository.findById(entity.getId());

        assertTrue(result.isPresent());
        assertEquals(entity.getId(), result.get().getId());
    }

    @Test
    void findByIdCognitoShouldMapEntityWhenPresent() {
        UsuarioEntity entity = userEntity();
        springDataUsuarioRepository.save(entity);
        entityManager.flush();
        entityManager.clear();

        Optional<Usuario> result = repository.findByIdCognito(entity.getIdCognito());

        assertTrue(result.isPresent());
        assertEquals(entity.getId(), result.get().getId());
    }

    @Test
    void findByEmailShouldMapEntityWhenPresent() {
        UsuarioEntity entity = userEntity();
        springDataUsuarioRepository.save(entity);
        entityManager.flush();
        entityManager.clear();

        Optional<Usuario> result = repository.findByEmail(entity.getEmail());

        assertTrue(result.isPresent());
        assertEquals(entity.getId(), result.get().getId());
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

    private void assertUsuarioRelationsWereSaved(Usuario command, Usuario result, ColegioEntity colegioEntity, AlumnoEntity alumnoEntity, TutorEntity tutorEntity, KiosqueroEntity kiosqueroEntity) {
        UsuarioEntity saved = springDataUsuarioRepository.findById(result.getId()).orElseThrow();
        assertEquals(result.getId(), saved.getId());
        assertEquals(command.getIdCognito(), saved.getIdCognito());
        assertEquals(colegioEntity.getId(), saved.getColegioEntity().getId());
        assertEquals(alumnoEntity.getId(), saved.getAlumnoEntity().getId());
        assertEquals(tutorEntity.getId(), saved.getTutor().getId());
        assertEquals(kiosqueroEntity.getId(), saved.getKiosquero().getId());
    }
}
