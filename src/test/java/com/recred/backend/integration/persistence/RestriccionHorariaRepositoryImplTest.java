package com.recred.backend.integration.persistence;

import com.recred.backend.domain.models.CategoriaProducto;
import com.recred.backend.domain.models.ClasificacionSalud;
import com.recred.backend.domain.models.FranjaHoraria;
import com.recred.backend.domain.models.RestriccionHoraria;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.infrastructure.entities.*;
import com.recred.backend.infrastructure.persistence.RestriccionHorariaRepositoryImpl;
import com.recred.backend.infrastructure.persistence.SpringDataRestriccionHorariaRepository;
import com.recred.backend.integration.AbstractIntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Transactional
class RestriccionHorariaRepositoryImplTest extends AbstractIntegrationTest {

    @Autowired
    private RestriccionHorariaRepositoryImpl repository;

    @Autowired
    private SpringDataRestriccionHorariaRepository springDataRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void saveShouldCreateEntityWithAllReferences() {
        AlumnoEntity student = generateStudent();
        FranjaHorariaEntity timeSlot = generateTimeSlot();
        CategoriaProductoEntity category = generateCategory();
        ClasificacionSaludEntity health = generateHealth();
        UsuarioEntity user = generateUser(RolUsuario.PADRE);

        RestriccionHoraria domain = createDomainRestriction(null, student.getId(), timeSlot.getId(), category.getId(), health.getId(), user.getId(), true);

        RestriccionHoraria result = repository.save(domain);
        entityManager.flush();
        entityManager.clear();

        assertNotNull(result.getId());
        RestriccionHorariaEntity saved = springDataRepository.findById(result.getId()).orElseThrow();
        
        assertEquals(student.getId(), saved.getAlumnoEntity().getId());
        assertEquals(timeSlot.getId(), saved.getFranjaHorariaEntity().getId());
        assertEquals(category.getId(), saved.getCategoria().getId());
        assertEquals(health.getId(), saved.getClasificacionSaludEntity().getId());
        assertEquals(user.getId(), saved.getCreadoPorUsuarioEntity().getId());
    }

    @Test
    void saveShouldUpdateExistingEntityAndClearOptionalReferences() {
        AlumnoEntity student = generateStudent();
        FranjaHorariaEntity timeSlot = generateTimeSlot();
        CategoriaProductoEntity category = generateCategory();
        ClasificacionSaludEntity health = generateHealth();
        UsuarioEntity user = generateUser(RolUsuario.PADRE);

        RestriccionHorariaEntity existing = generateRestrictionEntity(student, timeSlot, category, health, user, true);
        entityManager.flush();

        RestriccionHoraria domain = createDomainRestriction(existing.getId(), student.getId(), timeSlot.getId(), null, null, user.getId(), false);

        repository.save(domain);
        entityManager.flush();
        entityManager.clear();

        RestriccionHorariaEntity updated = springDataRepository.findById(existing.getId()).orElseThrow();
        assertFalse(updated.getActiva());
        assertNull(updated.getCategoria());
        assertNull(updated.getClasificacionSaludEntity());
        assertNotNull(updated.getCreadoPorUsuarioEntity());
    }

    @Test
    void findByIdShouldMapEntity() {
        RestriccionHorariaEntity existing = generateRestrictionEntity(generateStudent(), generateTimeSlot(), null, null, generateUser(RolUsuario.PADRE), true);
        entityManager.flush();

        Optional<RestriccionHoraria> result = repository.findById(existing.getId());

        assertTrue(result.isPresent());
        assertEquals(existing.getId(), result.get().getId());
    }

    @Test
    void findAllByAlumnoIdAndActivaShouldMapEntities() {
        AlumnoEntity student = generateStudent();
        FranjaHorariaEntity timeSlot = generateTimeSlot();
        UsuarioEntity user = generateUser(RolUsuario.PADRE);
        
        RestriccionHorariaEntity active = generateRestrictionEntity(student, timeSlot, null, null, user, true);
        RestriccionHorariaEntity inactive = generateRestrictionEntity(student, timeSlot, null, null, user, false);
        entityManager.flush();

        List<RestriccionHoraria> result = repository.findAllByAlumnoIdAndActiva(student.getId(), true);

        assertEquals(1, result.size());
        assertEquals(active.getId(), result.get(0).getId());
    }

    @Test
    void findAllByAlumnoEntityIdShouldMapEntities() {
        AlumnoEntity student = generateStudent();
        RestriccionHorariaEntity entity = generateRestrictionEntity(student, generateTimeSlot(), null, null, generateUser(RolUsuario.PADRE), true);
        entityManager.flush();

        List<RestriccionHoraria> result = repository.findAllByAlumnoEntityId(student.getId());

        assertEquals(1, result.size());
        assertEquals(entity.getId(), result.get(0).getId());
    }

    @Test
    void deleteByIdShouldDelegateToSpringDataRepository() {
        RestriccionHorariaEntity entity = generateRestrictionEntity(generateStudent(), generateTimeSlot(), null, null, generateUser(RolUsuario.PADRE), true);
        entityManager.flush();

        repository.deleteById(entity.getId());
        entityManager.flush();
        entityManager.clear();

        assertTrue(springDataRepository.findById(entity.getId()).isEmpty());
    }

    private RestriccionHorariaEntity generateRestrictionEntity(AlumnoEntity student, FranjaHorariaEntity timeSlot, CategoriaProductoEntity category, ClasificacionSaludEntity health, UsuarioEntity user, boolean active) {
        RestriccionHorariaEntity entity = new RestriccionHorariaEntity();
        entity.setAlumnoEntity(student);
        entity.setFranjaHorariaEntity(timeSlot);
        entity.setCategoria(category);
        entity.setClasificacionSaludEntity(health);
        entity.setCreadoPorUsuarioEntity(user);
        entity.setActiva(active);
        springDataRepository.save(entity);
        return entity;
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
        UsuarioEntity user = generateUser(RolUsuario.ALUMNO);
        AlumnoEntity student = new AlumnoEntity();
        student.setUsuario(user);
        student.setActivo(true);
        entityManager.persist(student);
        return student;
    }

    private FranjaHorariaEntity generateTimeSlot() {
        ColegioEntity school = new ColegioEntity();
        school.setNombre("School " + UUID.randomUUID().toString().substring(0, 5));
        school.setCue("CUE-" + UUID.randomUUID().toString().substring(0, 5));
        school.setActivo(true);
        entityManager.persist(school);

        FranjaHorariaEntity timeSlot = new FranjaHorariaEntity();
        timeSlot.setColegioEntity(school);
        timeSlot.setDescripcion("Recess");
        timeSlot.setHoraInicio(LocalTime.of(10, 0));
        timeSlot.setHoraFin(LocalTime.of(10, 15));
        timeSlot.setActivo(true);
        entityManager.persist(timeSlot);
        return timeSlot;
    }

    private CategoriaProductoEntity generateCategory() {
        CategoriaProductoEntity category = new CategoriaProductoEntity();
        category.setDescripcion("Sweets");
        category.setActivo(true);
        entityManager.persist(category);
        return category;
    }

    private ClasificacionSaludEntity generateHealth() {
        ClasificacionSaludEntity health = new ClasificacionSaludEntity();
        health.setDescripcion("High Sugar");
        entityManager.persist(health);
        return health;
    }

    private RestriccionHoraria createDomainRestriction(UUID id, UUID studentId, UUID timeSlotId, UUID categoryId, UUID healthId, UUID userId, boolean active) {
        RestriccionHoraria domain = new RestriccionHoraria();
        domain.setId(id);
        domain.setAlumnoId(studentId);
        
        FranjaHoraria franja = new FranjaHoraria();
        franja.setId(timeSlotId);
        domain.setFranjaHoraria(franja);
        
        domain.setActiva(active);
        
        if (categoryId != null) {
            CategoriaProducto cat = new CategoriaProducto();
            cat.setId(categoryId);
            domain.setCategoria(cat);
        }
        
        if (healthId != null) {
            ClasificacionSalud cs = new ClasificacionSalud();
            cs.setId(healthId);
            domain.setClasificacionSalud(cs);
        }
        
        domain.setCreadoPorUsuarioId(userId);
        return domain;
    }
}
