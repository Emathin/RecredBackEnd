package com.recred.backend.integration.persistence;

import com.recred.backend.domain.models.Colegio;
import com.recred.backend.infrastructure.entities.ColegioEntity;
import com.recred.backend.infrastructure.persistence.ColegioRepositoryImpl;
import com.recred.backend.infrastructure.persistence.SpringDataColegioRepository;
import com.recred.backend.integration.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Transactional
class ColegioRepositoryImplIT extends AbstractIntegrationTest {

    @Autowired
    private SpringDataColegioRepository springDataColegioRepository;

    @Autowired
    private ColegioRepositoryImpl repository;

    private ColegioEntity cachedActiveSchool;
    private ColegioEntity cachedAnotherActiveSchool;

    @Test
    void findAllActive_ShouldMapActiveSchoolsToDomain() {
        ColegioEntity first = givenActiveSchool();
        ColegioEntity second = givenAnotherActiveSchool();

        List<Colegio> result = whenFindingAllActive();

        thenSchoolsArePresentInResult(result, first, second);
    }

    @Test
    void existsActiveById_ShouldDelegateToSpringDataRepository() {
        ColegioEntity school = givenActiveSchool();

        boolean exists = whenCheckingIfActiveExistsById(school.getId());

        assertTrue(exists);
    }

    @Test
    void existsByEmail_ShouldReturnTrue_WhenExists() {
        ColegioEntity school = givenSchoolWithEmail("north@school.com");

        boolean exists = whenCheckingIfExistsByEmail(school.getEmail());

        assertTrue(exists);
    }

    @Test
    void existsByCue_ShouldReturnTrue_WhenExists() {
        ColegioEntity school = givenActiveSchool();

        boolean exists = whenCheckingIfExistsByCue(school.getCue());

        assertTrue(exists);
    }

    @Test
    void save_ShouldPersistNewColegio() {
        Colegio colegio = givenNewSchoolDomain();

        Colegio saved = whenSavingSchool(colegio);

        thenSchoolIsPersisted(saved);
    }

    private ColegioEntity givenActiveSchool() {
        return generateActiveSchool();
    }

    private ColegioEntity givenAnotherActiveSchool() {
        return generateAnotherActiveSchool();
    }

    private ColegioEntity givenSchoolWithEmail(String email) {
        ColegioEntity school = generateActiveSchool();
        school.setEmail(email);
        return springDataColegioRepository.saveAndFlush(school);
    }

    private Colegio givenNewSchoolDomain() {
        Colegio colegio = new Colegio();
        colegio.setNombre("New School");
        colegio.setCue("999999");
        colegio.setEmail("new@school.com");
        colegio.setActivo(true);
        return colegio;
    }

    private List<Colegio> whenFindingAllActive() {
        return repository.findAllActive();
    }

    private boolean whenCheckingIfActiveExistsById(UUID id) {
        return repository.existsActiveById(id);
    }

    private boolean whenCheckingIfExistsByEmail(String email) {
        return repository.existsByEmail(email);
    }

    private boolean whenCheckingIfExistsByCue(String cue) {
        return repository.existsByCue(cue);
    }

    private Colegio whenSavingSchool(Colegio colegio) {
        return repository.save(colegio);
    }

    private void thenSchoolsArePresentInResult(List<Colegio> result, ColegioEntity first, ColegioEntity second) {
        assertTrue(result.size() >= 2);
        assertTrue(result.stream().anyMatch(c -> c.getId().equals(first.getId()) && "North School".equals(c.getNombre())));
        assertTrue(result.stream().anyMatch(c -> c.getId().equals(second.getId()) && "South School".equals(c.getNombre())));
    }

    private void thenSchoolIsPersisted(Colegio saved) {
        assertTrue(springDataColegioRepository.findById(saved.getId()).isPresent());
        assertEquals("New School", saved.getNombre());
    }

    private ColegioEntity generateActiveSchool() {
        if (this.cachedActiveSchool == null) {
            ColegioEntity entity = new ColegioEntity();
            entity.setNombre("North School");
            entity.setCue("123456");
            entity.setActivo(true);
            this.cachedActiveSchool = springDataColegioRepository.saveAndFlush(entity);
        }
        return this.cachedActiveSchool;
    }

    private ColegioEntity generateAnotherActiveSchool() {
        if (this.cachedAnotherActiveSchool == null) {
            ColegioEntity entity = new ColegioEntity();
            entity.setNombre("South School");
            entity.setCue("1234568");
            entity.setActivo(true);
            this.cachedAnotherActiveSchool = springDataColegioRepository.saveAndFlush(entity);
        }
        return this.cachedAnotherActiveSchool;
    }
}
