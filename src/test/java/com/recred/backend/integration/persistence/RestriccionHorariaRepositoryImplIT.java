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
class RestriccionHorariaRepositoryImplIT extends AbstractIntegrationTest {

    @Autowired
    private RestriccionHorariaRepositoryImpl repository;

    @Autowired
    private SpringDataRestriccionHorariaRepository springDataRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void saveShouldCreateEntityWithAllReferences() {
        RestriccionHoraria domain = givenNewRestrictionDomain();

        RestriccionHoraria result = whenSavingRestriction(domain);

        thenRestrictionIsPersistedWithAllReferences(result, domain);
    }

    @Test
    void saveShouldUpdateExistingEntityAndClearOptionalReferences() {
        RestriccionHoraria domain = givenExistingRestrictionForUpdate();

        whenSavingRestriction(domain);

        thenRestrictionIsUpdatedAndOptionalReferencesCleared(domain.getId());
    }

    @Test
    void findByIdShouldMapEntity() {
        RestriccionHorariaEntity existing = givenActiveRestriction();

        Optional<RestriccionHoraria> result = whenFindingRestrictionById(existing.getId());

        thenRestrictionIsFound(result, existing.getId());
    }

    @Test
    void findAllByAlumnoIdAndActivaShouldMapEntities() {
        AlumnoEntity student = givenStudentWithActiveAndInactiveRestrictions();

        List<RestriccionHoraria> result = whenFindingActiveRestrictionsByStudent(student.getId());

        thenOnlyActiveRestrictionsAreReturned(result);
    }

    @Test
    void findAllByAlumnoEntityIdShouldMapEntities() {
        AlumnoEntity student = givenStudentWithRestrictions();

        List<RestriccionHoraria> result = whenFindingAllRestrictionsByStudent(student.getId());

        thenAllRestrictionsAreReturned(result);
    }

    @Test
    void deleteByIdShouldDelegateToSpringDataRepository() {
        RestriccionHorariaEntity entity = givenActiveRestriction();

        whenDeletingRestrictionById(entity.getId());

        thenRestrictionIsDeleted(entity.getId());
    }

    private RestriccionHoraria givenNewRestrictionDomain() {
        AlumnoEntity student = generateStudent();
        FranjaHorariaEntity timeSlot = generateTimeSlot();
        CategoriaProductoEntity category = generateCategory();
        ClasificacionSaludEntity health = generateHealth();
        UsuarioEntity user = generateUser(RolUsuario.PADRE);
        return createDomainRestriction(null, student.getId(), timeSlot.getId(), category.getId(), health.getId(), user.getId(), true);
    }

    private RestriccionHoraria givenExistingRestrictionForUpdate() {
        AlumnoEntity student = generateStudent();
        FranjaHorariaEntity timeSlot = generateTimeSlot();
        CategoriaProductoEntity category = generateCategory();
        ClasificacionSaludEntity health = generateHealth();
        UsuarioEntity user = generateUser(RolUsuario.PADRE);

        RestriccionHorariaEntity existing = generateRestrictionEntity(student, timeSlot, category, health, user, true);
        entityManager.flush();

        return createDomainRestriction(existing.getId(), student.getId(), timeSlot.getId(), null, null, user.getId(), false);
    }

    private RestriccionHorariaEntity givenActiveRestriction() {
        RestriccionHorariaEntity existing = generateRestrictionEntity(generateStudent(), generateTimeSlot(), null, null, generateUser(RolUsuario.PADRE), true);
        entityManager.flush();
        return existing;
    }

    private AlumnoEntity givenStudentWithActiveAndInactiveRestrictions() {
        AlumnoEntity student = generateStudent();
        FranjaHorariaEntity timeSlot = generateTimeSlot();
        UsuarioEntity user = generateUser(RolUsuario.PADRE);
        
        generateRestrictionEntity(student, timeSlot, null, null, user, true);
        generateRestrictionEntity(student, timeSlot, null, null, user, false);
        entityManager.flush();
        return student;
    }

    private AlumnoEntity givenStudentWithRestrictions() {
        AlumnoEntity student = generateStudent();
        generateRestrictionEntity(student, generateTimeSlot(), null, null, generateUser(RolUsuario.PADRE), true);
        entityManager.flush();
        return student;
    }

    private RestriccionHoraria whenSavingRestriction(RestriccionHoraria domain) {
        RestriccionHoraria result = repository.save(domain);
        entityManager.flush();
        entityManager.clear();
        return result;
    }

    private Optional<RestriccionHoraria> whenFindingRestrictionById(UUID id) {
        return repository.findById(id);
    }

    private List<RestriccionHoraria> whenFindingActiveRestrictionsByStudent(UUID studentId) {
        return repository.findAllByAlumnoIdAndActiva(studentId, true);
    }

    private List<RestriccionHoraria> whenFindingAllRestrictionsByStudent(UUID studentId) {
        return repository.findAllByAlumnoEntityId(studentId);
    }

    private void whenDeletingRestrictionById(UUID id) {
        repository.deleteById(id);
        entityManager.flush();
        entityManager.clear();
    }

    private void thenRestrictionIsPersistedWithAllReferences(RestriccionHoraria result, RestriccionHoraria domain) {
        assertNotNull(result.getId());
        RestriccionHorariaEntity saved = springDataRepository.findById(result.getId()).orElseThrow();
        
        assertEquals(domain.getAlumnoId(), saved.getAlumnoEntity().getId());
        assertEquals(domain.getFranjaHoraria().getId(), saved.getFranjaHorariaEntity().getId());
        assertEquals(domain.getCategoria().getId(), saved.getCategoria().getId());
        assertEquals(domain.getClasificacionSalud().getId(), saved.getClasificacionSaludEntity().getId());
        assertEquals(domain.getCreadoPorUsuarioId(), saved.getCreadoPorUsuarioEntity().getId());
    }

    private void thenRestrictionIsUpdatedAndOptionalReferencesCleared(UUID id) {
        RestriccionHorariaEntity updated = springDataRepository.findById(id).orElseThrow();
        assertFalse(updated.getActiva());
        assertNull(updated.getCategoria());
        assertNull(updated.getClasificacionSaludEntity());
        assertNotNull(updated.getCreadoPorUsuarioEntity());
    }

    private void thenRestrictionIsFound(Optional<RestriccionHoraria> result, UUID expectedId) {
        assertTrue(result.isPresent());
        assertEquals(expectedId, result.get().getId());
    }

    private void thenOnlyActiveRestrictionsAreReturned(List<RestriccionHoraria> result) {
        assertEquals(1, result.size());
    }

    private void thenAllRestrictionsAreReturned(List<RestriccionHoraria> result) {
        assertEquals(1, result.size());
    }

    private void thenRestrictionIsDeleted(UUID id) {
        assertTrue(springDataRepository.findById(id).isEmpty());
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
